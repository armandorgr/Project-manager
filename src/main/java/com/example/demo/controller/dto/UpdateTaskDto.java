package com.example.demo.controller.dto;

import com.example.demo.model.TaskPriority;
import com.example.demo.model.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(
        name = "UpdateTaskDto",
        description = "DTO utilizado para actualizar los datos de una tarea. Todos los campos son opcionales."
)
public record UpdateTaskDto(

        @Schema(
                description = "Nuevo nombre de la tarea (opcional).",
                example = "Implementar autenticación con JWT",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String name,

        @Schema(
                description = "Nueva descripción de la tarea (opcional).",
                example = "Configurar autenticación con tokens JWT y proteger endpoints sensibles.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String description,

        @Schema(
                description = "Nuevo estado de la tarea (opcional).",
                example = "IN_PROGRESS",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        TaskStatus status,

        @Schema(
                description = "Nueva prioridad de la tarea (opcional).",
                example = "HIGH",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        TaskPriority priority,

        @Schema(
                description = "Nueva fecha límite de la tarea (opcional, en formato UTC).",
                example = "2025-12-31T23:59:59Z",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Instant dueDate,

        @Schema(
                description = "Nuevo usuario asignado a la tarea (opcional).",
                example = "c8b6d2b2-3f8b-4b5e-bf67-1a5c9e2393a1",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        UUID assignedUser
) {}
