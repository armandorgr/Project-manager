package com.example.demo.controller;

import com.example.demo.controller.requests.RegisterRequest;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.TokenBlacklistService;
import com.example.demo.controller.requests.LoginRequest;
import com.example.demo.controller.requests.LogoutRequest;
import com.example.demo.controller.requests.TokenRefreshRequest;
import com.example.demo.controller.responses.ApiResponse;
import com.example.demo.controller.responses.JwtResponse;
import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;
import com.example.demo.security.JwtTokenUtil;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.controller.exception.TokenRefreshException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public ResponseEntity<ApiResponse<String>> register(@RequestBody @Valid RegisterRequest request) {
        try {
            this.logger.debug(String.format("Intento de registro de usuario con username: %s", request.getUsername()));
            userDetailsService.registerUser(request.getUsername(), this.passwordEncoder.encode(request.getPassword()), request.getEmail());
        } catch (DuplicateKeyException e) {
            this.logger.debug(String.format("Excepcion de tipo %s", e.getClass()));
            throw e;
        }
        ApiResponse<String> response = new ApiResponse<>("Success", "Usuario registrado correctamente", null, null);
        this.logger.debug(String.format("Registro correcto de usuario con username: %s", request.getUsername()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@RequestBody @Valid LoginRequest request) {
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
        ApiResponse<JwtResponse> response = new ApiResponse<>("Success", "Inicio de sesión correcto", new JwtResponse(token, refreshToken.getToken()), null);
        this.logger.debug(String.format("Inicio de sesión correcto por usuario con username: %s", request.getUsername()));
        if(blacklistService.isTokenBlackListed(token)){
            blacklistService.unBlackListToken(token);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestBody @Valid LogoutRequest request, Authentication authentication) {
        refreshTokenService.findByToken(request.refreshToken())
                .ifPresent(refreshToken ->
                        refreshTokenService.deleteRefreshToken(refreshToken.getId()));
        String accessToken = request.accessToken();
        if (accessToken != null && !accessToken.isBlank()) {
            long expMillis = jwtTokenUtil.extractExpiration(accessToken).getTime();
            long currentMillis = System.currentTimeMillis();
            long ttl = Math.max(0, expMillis - currentMillis);

            blacklistService.blacklistToken(accessToken, ttl);
        }
        SecurityContextHolder.clearContext();
        ApiResponse<String> response = new ApiResponse<>("Success", "Sesión cerrada correctamente", null, null);
        this.logger.debug(String.format("Sesión cerrada por usuario con username: %s", authentication.getCredentials()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@RequestBody @Valid TokenRefreshRequest request) {
        this.logger.debug(String.format("Intento de refresco de token por usuario con token: %s", request.getRefreshToken()));
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken()).orElseThrow(() -> new TokenRefreshException("Token de refresco inválido"));
        refreshToken = refreshTokenService.verifyExpiration(refreshToken);
        User user = (User) userDetailsService.loadUserById(refreshToken.getUser().getId());
        String newAccessToken = jwtTokenUtil.generateToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);
        this.logger.debug(String.format("Token refrescado"));
        return ResponseEntity.ok(new ApiResponse<>("Success", "Token refreshed successfully", new JwtResponse(newAccessToken, newRefreshToken.getToken()), null));
    }
}

