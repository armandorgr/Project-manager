package com.example.demo.controller.dto;

import java.time.Instant;

public record UpdateProjectDto(
        String name,
        String description,
        Instant startDate,
        Instant endDate
) {
}
