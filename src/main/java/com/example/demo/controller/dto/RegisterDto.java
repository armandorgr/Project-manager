package com.example.demo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(
        name = "RegisterRequest",
        description = "DTO utilizado para registrar un nuevo usuario con nombre de usuario, email y contraseña."
)
public record RegisterDto(

        @Schema(
                description = "Nombre de usuario del nuevo usuario. Campo obligatorio.",
                example = "usuario123",
                requiredMode = RequiredMode.REQUIRED
        )
        @NotBlank(message = "Username is mandatory")
        String username,

        @Schema(
                description = "Correo electrónico del nuevo usuario. Campo obligatorio y debe ser un email válido.",
                example = "usuario@example.com",
                requiredMode = RequiredMode.REQUIRED
        )
        @Email
        @NotBlank(message = "Email is mandatory")
        String email,

        @Schema(
                description = "Contraseña del nuevo usuario. Campo obligatorio.",
                example = "P@ssw0rd123",
                requiredMode = RequiredMode.REQUIRED
        )
        @NotBlank(message = "Password is mandatory")
        String password
) {}
