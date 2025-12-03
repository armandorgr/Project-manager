package com.example.demo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;

@Schema(
        name = "LoginRequest",
        description = "DTO utilizado para realizar login con nombre de usuario y contraseña."
)
public record LoginDto(

        @Schema(
                description = "Nombre de usuario del usuario. Campo obligatorio.",
                example = "usuario123",
                requiredMode = RequiredMode.REQUIRED
        )
        @NotBlank(message = "Username is mandatory")
        String username,

        @Schema(
                description = "Contraseña del usuario. Campo obligatorio.",
                example = "P@ssw0rd123",
                requiredMode = RequiredMode.REQUIRED
        )
        @NotBlank(message = "Password is mandatory")
        String password
) {}
