package com.example.demo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(
        name = "CreateProjectDto",
        description = "DTO utilizado para crear un nuevo proyecto con nombre y descripción."
)
public record CreateProjectDto(

        @Schema(
                description = "Nombre del proyecto. No puede estar vacío.",
                example = "Sistema de Gestión de Tareas",
                requiredMode = RequiredMode.REQUIRED
        )
        @NotNull
        @NotBlank
        String name,

        @Schema(
                description = "Descripción breve del proyecto. No puede estar vacía.",
                example = "Aplicación para gestionar tareas y comentarios entre equipos.",
                requiredMode = RequiredMode.REQUIRED
        )
        @NotNull
        @NotBlank
        String description,

        @Schema(
                        description = "Fecha de inicio del proyecto. No puede estar vacía.",
                        requiredMode = RequiredMode.REQUIRED
                )
        @NotNull
        Instant startDate,

        @Schema(
                description = "Fecha de fin del proyecto. (Opcional)"
        )
        Instant endDate
) {}
