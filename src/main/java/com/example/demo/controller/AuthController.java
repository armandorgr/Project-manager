package com.example.demo.controller;

import com.example.demo.controller.anotations.auth.Tokens;
import com.example.demo.controller.requests.*;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.TokenBlacklistService;
import com.example.demo.controller.responses.ApiResponse;
import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;
import com.example.demo.security.JwtTokenUtil;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.controller.exception.TokenRefreshException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that handles authentication and user session management.
 * <p>
 * Endpoints:
 * - /register: User registration
 * - /login: User authentication and token generation
 * - /logout: Session termination and token invalidation
 * - /refresh: Token refresh with a valid refresh token
 */
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
            TokenBlacklistService blacklistService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.blacklistService = blacklistService;
    }

    /**
     * Registers a new user account.
     *
     * @param request DTO containing username, password, and email
     * @return ResponseEntity with HTTP 201 Created on success
     * @throws DuplicateKeyException if username or email already exist
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterRequest>> register(@RequestBody @Valid RegisterRequest request) {
        try {
            logger.debug("Attempting to register user with username: {}", request.getUsername());
            User user = (User) userDetailsService.registerUser(
                    request.getUsername(),
                    passwordEncoder.encode(request.getPassword()),
                    request.getEmail()
            );

            ApiResponse<RegisterRequest> response =
                    new ApiResponse<>("SUCCESS", "User registered successfully", request, null);
            logger.info("User '{}' registered successfully", user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (DuplicateKeyException e) {
            logger.warn("Duplicate key during registration for user '{}'", request.getUsername());
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
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response
    ) {
        logger.debug("Login attempt by user: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
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

        ApiResponse<String> apiResponse =
                new ApiResponse<>("SUCCESS", "Login successful", null, null);
        logger.info("User '{}' logged in successfully", userDetails.getUsername());
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Logs out the current user by deleting refresh token and blacklisting access token.
     * Cookies are cleared from the client.
     *
     * @param tokens          Object containing current access and refresh tokens
     * @param response        HttpServletResponse for clearing cookies
     * @param authentication  Current authenticated user
     * @return ResponseEntity with HTTP 204 No Content (no body needed)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Tokens TokensObj tokens,
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
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken(
            @Tokens TokensObj tokens,
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

        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Token refreshed successfully", null, null));
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
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false); // TODO: set true in production
        accessCookie.setPath("/");
        accessCookie.setMaxAge(revoke ? 0 : (int) (accessTokenExpiration / 1000));

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // TODO: set true in production
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge(revoke ? 0 : (int) (refreshTokenExpiration / 1000));

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }
}

