package com.example.demo.controller.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponseDto(
        UUID id,
        String name,
        String description,
        Instant startDate,
        Instant endDate
) {
}
