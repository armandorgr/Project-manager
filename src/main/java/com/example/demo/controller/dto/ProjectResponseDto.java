package com.example.demo.controller.dto;

import com.example.demo.model.ProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(
        name = "ProjectResponseDto",
        description = "Representa la información detallada de un proyecto."
)
public record ProjectResponseDto(

        @Schema(
                description = "Identificador único del proyecto.",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "Nombre del proyecto.",
                example = "Sistema de Gestión de Tareas"
        )
        String name,

        @Schema(
                description = "Descripción del proyecto.",
                example = "Aplicación para gestionar tareas, comentarios y usuarios en equipos de trabajo."
        )
        String description,

        @Schema(
                description = "Fecha de inicio del proyecto (en formato UTC).",
                example = "2025-01-15T08:00:00Z"
        )
        Instant startDate,

        @Schema(
                description = "Fecha estimada de finalización del proyecto (en formato UTC).",
                example = "2025-06-30T17:00:00Z"
        )
        Instant endDate,

        @Schema(
                description = "Rol que tiene el usuario autenticado dentro del proyecto",
                example = "ADMIN"
        )
        ProjectRole role
) {
}
