package com.example.demo.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProjectDto(
        @NotNull()
        @NotBlank()
        String name,
        @NotNull()
        @NotBlank()
        String description) {
}
