package com.example.demo.controller.dto;

import com.example.demo.controller.anotations.validation.AtLeastOneField;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@AtLeastOneField(first = "username", second = "email",
        message = "Debe proporcionarse al menos un username o un email.")
@Schema(
        name = "InvitationDto",
        description = "DTO utilizado para buscar un usuario mediante nombre de usuario o correo electrónico."
)
public record InvitationDto(

        @Schema(
                description = "Nombre de usuario del destinatario de la invitación (opcional si se proporciona email).",
                example = "usuario123",
                requiredMode = RequiredMode.NOT_REQUIRED
        )
        String username,

        @Schema(
                description = "Correo electrónico del destinatario de la invitación (opcional si se proporciona username).",
                example = "usuario@example.com",
                requiredMode = RequiredMode.NOT_REQUIRED
        )
        String email
) {
}
