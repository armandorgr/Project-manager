package com.example.demo.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectQueryDto(@NotBlank(message = "Query can not be empty") String query) {
}
