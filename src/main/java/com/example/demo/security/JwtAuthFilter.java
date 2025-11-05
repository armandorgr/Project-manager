package com.example.demo.security;

import com.example.demo.controller.responses.ApiResponse;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.TokenBlacklistService;
import com.example.demo.controller.exception.BlackListedTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtTokenUtil jwtTokenUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService blacklistService;

    public JwtAuthFilter(JwtTokenUtil jwtTokenUtil, CustomUserDetailsService userDetailsService, TokenBlacklistService blacklistService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.blacklistService = blacklistService;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String getJwtFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getMaxAge() == 0) return null;
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void sendError(HttpServletResponse response,
                           HttpStatus status,
                           String errorCode,
                           String errorMessage) throws IOException {
        ApiResponse<String> errorResponse = new ApiResponse<>(String.valueOf(status.value()), errorMessage, null, null);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String jwt = getJwtFromCookies(request);
        logger.debug(request.getRequestURI());

        // Si no hay token, dejar que otros filtros manejen la petición
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Verificar si el token está en la lista negra
            if (blacklistService.isTokenBlackListed(jwt)) {
                sendError(response, HttpStatus.UNAUTHORIZED, "TOKEN_REVOKED", "Token inválido por logout previo");
                return; // corta aquí
            }

            // Extraer y validar usuario
            final String username = jwtTokenUtil.extractUsername(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtTokenUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            // Continuar con la cadena normalmente
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
            sendError(response, HttpStatus.FORBIDDEN, "TOKEN_EXPIRED", "Token caducado");
        } catch (Exception ex) {
            logger.error("Unhandled exception in JwtAuthFilter", ex);
            filterChain.doFilter(request, response);
        }
    }

}