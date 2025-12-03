package com.example.demo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "CreateCommentDto",
        description = "DTO utilizado para crear un nuevo comentario asociado a una tarea."
)
public record CreateCommentDto(

        @Schema(
                description = "Contenido del comentario. No puede estar vacío.",
                example = "Este es un nuevo comentario sobre la tarea.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String content
) {}
