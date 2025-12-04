package com.example.demo.controller;

import com.example.demo.controller.anotations.auth.Tokens;
import com.example.demo.controller.dto.LoginDto;
import com.example.demo.controller.dto.RegisterDto;
import com.example.demo.controller.dto.UserResponseDto;
import com.example.demo.controller.requests.*;
import com.example.demo.controller.responses.Response;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.TokenBlacklistService;
import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;
import com.example.demo.security.JwtTokenUtil;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.controller.exception.TokenRefreshException;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller that handles authentication and user session management.
 * <p>
 * Endpoints:
 * - /register: User registration
 * - /login: User authentication and token generation
 * - /logout: Session termination and token invalidation
 * - /refresh: Token refresh with a valid refresh token
 */
@Tag(name = "Authentication", description = "Endpoints for user registration, login, logout, and token management")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService blacklistService;
    private final UserMapper userMapper;

    @Value("${jwt.access-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenExpiration;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenUtil jwtTokenUtil,
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            TokenBlacklistService blacklistService, UserMapper userMapper
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.blacklistService = blacklistService;
        this.userMapper = userMapper;
    }

    /**
     * Registers a new user account.
     *
     * @param request DTO containing username, password, and email
     * @return ResponseEntity with HTTP 201 Created on success
     * @throws DuplicateKeyException if username or email already exist
     */
    @Operation(
            summary = "Register a new user",
            description = "Registers a new user account with username, email and password.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "User registered successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Username or email already exists"
                    )
            },
            security = {}
    )
    @PostMapping("/register")
    public ResponseEntity<Response<RegisterDto>> register(@RequestBody @Valid RegisterDto request) {
        try {
            logger.debug("Attempting to register user with username: {}", request.username());
            User user = (User) userDetailsService.registerUser(
                    request.username(),
                    passwordEncoder.encode(request.password()),
                    request.email()
            );

            Response<RegisterDto> response =
                    new Response<>("SUCCESS", "User registered successfully", request, null);
            logger.info("User '{}' registered successfully", user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (DuplicateKeyException e) {
            logger.warn("Duplicate key during registration for user '{}'", request.username());
            throw e; // handled globally by exception handler
        }
    }

    /**
     * Authenticates a user and generates JWT access and refresh tokens.
     * Tokens are stored in HTTP-only cookies.
     *
     * @param request  DTO containing username and password
     * @param response HttpServletResponse for adding cookies
     * @return ResponseEntity with HTTP 200 OK on successful login
     */
    @Operation(
            summary = "Login a user",
            description = "Authenticates a user and returns JWT access and refresh tokens in cookies.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Login successful",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Invalid credentials"
                    )
            },
            security = {}
    )
    @PostMapping("/login")
    public ResponseEntity<Response<String>> login(
            @RequestBody @Valid LoginDto request,
            HttpServletResponse response
    ) {
        logger.debug("Login attempt by user: {}", request.username());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        User userDetails = (User) authentication.getPrincipal();
        String accessToken = jwtTokenUtil.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails);

        // Ensure token isn't blacklisted
        if (blacklistService.isTokenBlackListed(accessToken)) {
            blacklistService.unBlackListToken(accessToken);
        }

        addAuthCookies(response, accessToken, refreshToken.getToken(), false);

        Response<String> apiResponse =
                new Response<>("SUCCESS", "Login successful", null, null);
        logger.info("User '{}' logged in successfully", userDetails.getUsername());
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Logs out the current user by deleting refresh token and blacklisting access token.
     * Cookies are cleared from the client.
     *
     * @param tokens         Object containing current access and refresh tokens
     * @param response       HttpServletResponse for clearing cookies
     * @param authentication Current authenticated user
     * @return ResponseEntity with HTTP 204 No Content (no body needed)
     */
    @Operation(
            summary = "Logout a user",
            description = "Logs out the current user by deleting refresh token and blacklisting access token.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Logout successful"
                    )
            },
            security = {
                    @SecurityRequirement(name = "CookieAccess"),
                    @SecurityRequirement(name = "CookieRefresh")
            }
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) @Tokens TokensObj tokens,
            HttpServletResponse response,
            Authentication authentication
    ) {
        // Delete refresh token from DB if exists
        refreshTokenService.findByToken(tokens.getRefresh())
                .ifPresent(refreshToken ->
                        refreshTokenService.deleteRefreshToken(refreshToken.getId()));

        // Blacklist current access token until it expires
        String accessToken = tokens.getAccess();
        if (accessToken != null && !accessToken.isBlank()) {
            long expMillis = jwtTokenUtil.extractExpiration(accessToken).getTime();
            long currentMillis = System.currentTimeMillis();
            long ttl = Math.max(0, expMillis - currentMillis);
            blacklistService.blacklistToken(accessToken, ttl);
        }

        SecurityContextHolder.clearContext();
        addAuthCookies(response, tokens.getAccess(), tokens.getRefresh(), true);
        logger.info("User '{}' logged out successfully", authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * Refreshes JWT access and refresh tokens.
     *
     * @param tokens   Object containing current refresh token
     * @param response HttpServletResponse to send new cookies
     * @return ResponseEntity with HTTP 200 OK and new tokens in cookies
     */
    @Operation(
            summary = "Refresh JWT tokens",
            description = "Refreshes JWT access and refresh tokens using a valid refresh token.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Token refreshed successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Invalid refresh token"
                    )
            },
            security = {
                    @SecurityRequirement(name = "CookieAccess"),
                    @SecurityRequirement(name = "CookieRefresh")
            }
    )
    @PostMapping("/refresh")
    public ResponseEntity<Response<String>> refreshToken(
            @Parameter(hidden = true) @Tokens TokensObj tokens,
            HttpServletResponse response
    ) {
        logger.debug("Token refresh attempt with token: {}", tokens.getRefresh());

        RefreshToken refreshToken = refreshTokenService.findByToken(tokens.getRefresh())
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token"));

        refreshToken = refreshTokenService.verifyExpiration(refreshToken);
        User user = (User) userDetailsService.loadUserById(refreshToken.getUser().getId());

        String newAccessToken = jwtTokenUtil.generateToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        addAuthCookies(response, newAccessToken, newRefreshToken.getToken(), false);
        logger.info("Token refreshed successfully for user '{}'", user.getUsername());

        return ResponseEntity.ok(new Response<>("SUCCESS", "Token refreshed successfully", null, null));
    }

    @Operation(
            summary = "Get current user details",
            description = "Returns details of the currently authenticated user.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User details retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized"
                    )
            }
    )
    @GetMapping("/me")
    public ResponseEntity<Response<UserResponseDto>> getUserDetails(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(new Response<>("SUCCESS", "User data", this.userMapper.toResponse(currentUser), null));
    }

    /**
     * Adds or removes authentication cookies for access and refresh tokens.
     *
     * @param response     HTTP response object
     * @param accessToken  JWT access token
     * @param refreshToken JWT refresh token
     * @param revoke       If true, cookies will be deleted (maxAge=0)
     */
    private void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken, Boolean revoke) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", revoke ? "" : accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(revoke ? 0 : (accessTokenExpiration / 1000))
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", revoke ? "" : refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/auth/refresh")
                .maxAge(revoke ? 0 : (refreshTokenExpiration / 1000))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}

