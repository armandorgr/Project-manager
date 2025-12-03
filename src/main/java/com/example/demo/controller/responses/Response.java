package com.example.demo.controller.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "ApiResponse",
        description = "Respuesta genérica de la API que incluye estado, mensaje, datos y metadatos opcionales."
)
public record Response<T>(

        @Schema(
                description = "Estado de la respuesta (por ejemplo: SUCCESS o ERROR).",
                example = "SUCCESS"
        )
        String status,

        @Schema(
                description = "Mensaje descriptivo sobre el resultado de la operación.",
                example = "Operación completada correctamente."
        )
        String message,

        @Schema(
                description = "Datos devueltos por la operación. Puede ser cualquier tipo de objeto.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        T data,

        @Schema(
                description = "Metadatos opcionales adicionales relacionados con la respuesta.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Object metadata
) {}
