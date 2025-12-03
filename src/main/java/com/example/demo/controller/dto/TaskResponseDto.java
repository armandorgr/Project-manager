package com.example.demo.controller.dto;

import com.example.demo.model.TaskPriority;
import com.example.demo.model.TaskStatus;
import com.example.demo.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(
        name = "TaskResponseDto",
        description = "Representa la información detallada de una tarea dentro de un proyecto."
)
public record TaskResponseDto(

        @Schema(
                description = "I de la tarea.",
                example = "a7d2b1d0-5e3a-45f1-97c8-2e5f1c81d0c5"
        )
        UUID id,

        @Schema(
                description = "Nombre de la tarea.",
                example = "Implementar autenticación con JWT"
        )
        String name,

        @Schema(
                description = "Descripción detallada de la tarea.",
                example = "Configurar autenticación con tokens JWT y proteger endpoints sensibles."
        )
        String description,

        @Schema(
                description = "Estado actual de la tarea.",
                example = "PENDING"
        )
        TaskStatus status,

        @Schema(
                description = "Prioridad asignada a la tarea.",
                example = "HIGH"
        )
        TaskPriority priority,

        @Schema(
                description = "Fecha límite de la tarea (en formato UTC).",
                example = "2025-12-31T23:59:59Z"
        )
        Instant dueDate,

        @Schema(
                description = "Datos del usuario asignado a esta tarea"
        )
        UserResponseDto assignedUser,

        @Schema(
                description = "Identificador del proyecto al que pertenece la tarea.",
                example = "a7d2b1d0-5e3a-45f1-97c8-2e5f1c81d0c5"
        )
        UUID project
) {}
