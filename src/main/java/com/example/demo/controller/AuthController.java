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

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenUtil jwtTokenUtil,
                          CustomUserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder,
                          RefreshTokenService refreshTokenService,
                          TokenBlacklistService blacklistService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.blacklistService = blacklistService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterRequest>> register(@RequestBody @Valid RegisterRequest request) {
        try {
            this.logger.debug(String.format("Intento de registro de usuario con username: %s", request.getUsername()));
            User user = (User) userDetailsService.registerUser(request.getUsername(), this.passwordEncoder.encode(request.getPassword()), request.getEmail());
            ApiResponse<RegisterRequest> response = new ApiResponse<>("Success", "Usuario registrado correctamente", request, null);
            this.logger.debug(String.format("Registro correcto de usuario con username: %s", request.getUsername()));
            return ResponseEntity.ok(response);
        } catch (DuplicateKeyException e) {
            this.logger.debug(String.format("Excepcion de tipo %s", e.getClass()));
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        this.logger.debug(String.format("Intento de inicio de sesión por usuario con username: %s", request.getUsername()));
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        final User userDetails = (User) authentication.getPrincipal();
        final String token = jwtTokenUtil.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails);
        ApiResponse<String> apiResponse = new ApiResponse<>("Success", "Inicio de sesión correcto", null, null);
        this.logger.debug(String.format("Inicio de sesión correcto por usuario con username: %s", request.getUsername()));
        if (blacklistService.isTokenBlackListed(token)) {
            blacklistService.unBlackListToken(token);
        }
        addAuthCookies(response, token, refreshToken.getToken(), false); // add cookies to response
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Tokens TokensObj tokens, HttpServletResponse response, Authentication authentication) {
        this.logger.debug(tokens.toString());
        refreshTokenService.findByToken(tokens.getRefresh())
                .ifPresent(refreshToken ->
                        refreshTokenService.deleteRefreshToken(refreshToken.getId()));
        String accessToken = tokens.getAccess();
        if (accessToken != null && !accessToken.isBlank()) {
            long expMillis = jwtTokenUtil.extractExpiration(accessToken).getTime();
            long currentMillis = System.currentTimeMillis();
            long ttl = Math.max(0, expMillis - currentMillis);

            blacklistService.blacklistToken(accessToken, ttl);
        }
        SecurityContextHolder.clearContext();
        ApiResponse<String> apiResponse = new ApiResponse<>("Success", "Sesión cerrada correctamente", null, null);
        addAuthCookies(response, tokens.getAccess(), tokens.getRefresh(), true);
        this.logger.debug(String.format("Sesión cerrada por usuario con username: %s", authentication.getCredentials()));
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken(@Tokens TokensObj tokens, HttpServletResponse response) {
        this.logger.debug(String.format("Intento de refresco de token por usuario con token: %s", tokens.getRefresh()));
        RefreshToken refreshToken = refreshTokenService.findByToken(tokens.getRefresh()).orElseThrow(() -> new TokenRefreshException("Token de refresco inválido"));
        refreshToken = refreshTokenService.verifyExpiration(refreshToken);
        User user = (User) userDetailsService.loadUserById(refreshToken.getUser().getId());
        String newAccessToken = jwtTokenUtil.generateToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);
        addAuthCookies(response, newAccessToken, newRefreshToken.getToken(), false);
        this.logger.debug("Token refrescado");
        return ResponseEntity.ok(new ApiResponse<>("Success", "Token refreshed successfully", null, null));
    }

    private void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken, Boolean revoke) {
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false); // false during development
        accessCookie.setPath("/");
        accessCookie.setMaxAge(revoke ? 0 : this.accessTokenExpiration.intValue() / 1000);

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // false during development
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge(revoke ? 0 : this.refreshTokenExpiration.intValue() / 1000);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }
}

