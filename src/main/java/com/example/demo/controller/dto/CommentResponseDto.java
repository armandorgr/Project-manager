package com.example.demo.controller.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponseDto(
        UUID id,
        String content,
        Instant createdAt,
        Instant updatedAt,
        UUID user,
        UUID task
) {}
