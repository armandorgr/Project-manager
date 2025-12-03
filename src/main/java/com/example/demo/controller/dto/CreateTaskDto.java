package com.example.demo.controller.dto;

import com.example.demo.model.TaskPriority;
import com.example.demo.model.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Schema(
        name = "CreateTaskDto",
        description = "DTO utilizado para crear una nueva tarea con su información principal."
)
public record CreateTaskDto(

        @Schema(
                description = "Nombre de la tarea. No puede estar vacío.",
                example = "Implementar autenticación con JWT",
                requiredMode = RequiredMode.REQUIRED
        )
        @NotBlank
        @NotNull
        String name,

        @Schema(
                description = "Descripción detallada de la tarea. No puede estar vacía.",
                example = "Configurar el sistema de autenticación usando tokens JWT y proteger los endpoints.",
                requiredMode = RequiredMode.REQUIRED
        )
        @NotBlank
        @NotNull
        String description,

        @Schema(
                description = "Estado inicial de la tarea.",
                example = "NOT_STARTED",
                requiredMode = RequiredMode.REQUIRED
        )
        @NotNull
        TaskStatus status,

        @Schema(
                description = "Prioridad de la tarea.",
                example = "HIGH",
                requiredMode = RequiredMode.REQUIRED
        )
        @NotNull
        TaskPriority priority,

        @Schema(
                description = "Fecha límite de la tarea en formato UTC.",
                example = "2025-12-31T23:59:59Z",
                requiredMode = RequiredMode.REQUIRED
        )
        @NotNull
        Instant dueDate,

        @Schema(
                description = "Identificador del usuario asignado a la tarea (opcional).",
                example = "c8b6d2b2-3f8b-4b5e-bf67-1a5c9e2393a1",
                requiredMode = RequiredMode.NOT_REQUIRED
        )
        UUID userId
) {}
