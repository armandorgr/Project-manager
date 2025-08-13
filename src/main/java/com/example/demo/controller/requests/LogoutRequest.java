package com.example.demo.controller.requests;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "Access token is mandatory")
        String accessToken,
        @NotBlank(message = "RefreshToken is mandatory")
        String refreshToken
) { }
