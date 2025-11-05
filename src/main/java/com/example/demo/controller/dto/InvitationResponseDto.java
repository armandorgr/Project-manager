package com.example.demo.controller.dto;

import java.util.UUID;

public record InvitationResponseDto(
        UUID sender,
        UUID projectId,
        String message
) {
}
