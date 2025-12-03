package com.example.demo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(
        name = "UpdateProjectDto",
        description = "DTO utilizado para actualizar los datos de un proyecto. Todos los campos son opcionales."
)
public record UpdateProjectDto(

        @Schema(
                description = "Nuevo nombre del proyecto (opcional).",
                example = "Sistema de Gestión de Tareas",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String name,

        @Schema(
                description = "Nueva descripción del proyecto (opcional).",
                example = "Aplicación para gestionar tareas y comentarios en equipos de trabajo.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String description,

        @Schema(
                description = "Nueva fecha de inicio del proyecto (opcional, en formato UTC).",
                example = "2025-01-15T08:00:00Z",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Instant startDate,

        @Schema(
                description = "Nueva fecha de finalización del proyecto (opcional, en formato UTC).",
                example = "2025-06-30T17:00:00Z",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Instant endDate
) {}
