package com.example.demo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(
        name = "InvitationResponseDto",
        description = "Representa la respuesta de una invitación enviada a un usuario para unirse a un proyecto."
)
public record InvitationResponseDto(

        @Schema(
                description = "Identificador del usuario que envió la invitación.",
                example = "c8b6d2b2-3f8b-4b5e-bf67-1a5c9e2393a1"
        )
        UUID sender,

        @Schema(
                description = "Identificador del proyecto al que se invita al usuario.",
                example = "a7d2b1d0-5e3a-45f1-97c8-2e5f1c81d0c5"
        )
        UUID projectId,

        @Schema(
                description = "Mensaje opcional incluido en la invitación.",
                example = "¡Nos gustaría que te unas a nuestro proyecto!"
        )
        String message
) {}
