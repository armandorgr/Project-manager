package com.example.demo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(
        name = "CommentResponseDto",
        description = "Representa la respuesta de un comentario asociado a una tarea y un usuario."
)
public record   CommentResponseDto(

        @Schema(
                description = "Identificador único del comentario.",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "Contenido del comentario.",
                example = "Este es un comentario de prueba."
        )
        String content,

        @Schema(
                description = "Fecha y hora en que se creó el comentario (en formato UTC).",
                example = "2025-11-12T10:15:30Z"
        )
        Instant createdAt,

        @Schema(
                description = "Fecha y hora de la última actualización del comentario (en formato UTC).",
                example = "2025-11-12T11:45:10Z"
        )
        Instant updatedAt,

        @Schema(
                description = "Identificador único del usuario que realizó el comentario.",
                example = "c8b6d2b2-3f8b-4b5e-bf67-1a5c9e2393a1"
        )
        UserResponseDto user
) {}
