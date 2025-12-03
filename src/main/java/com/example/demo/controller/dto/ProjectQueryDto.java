package com.example.demo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;

@Schema(
        name = "ProjectQueryDto",
        description = "DTO utilizado para realizar búsquedas de proyectos por nombre o término de búsqueda."
)
public record ProjectQueryDto(

        @Schema(
                description = "Término de búsqueda utilizado para filtrar proyectos. No puede estar vacío.",
                example = "Gestión de Tareas",
                requiredMode = RequiredMode.REQUIRED
        )
        @NotBlank(message = "Query can not be empty")
        String query
) {}
