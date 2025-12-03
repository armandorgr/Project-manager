package com.example.demo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(
        name = "UserResponseDto",
        description = "Representa la información básica de un usuario."
)
public record UserResponseDto(

        @Schema(
                description = "Id del usuario.",
                example = "c8b6d2b2-3f8b-4b5e-bf67-1a5c9e2393a1",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        UUID id,

        @Schema(
                description = "Nombre de usuario del usuario.",
                example = "usuario123",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String username,

        @Schema(
                description = "Correo electrónico del usuario.",
                example = "usuario@example.com",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String email
) {}
