package com.example.demo.controller.dto;

import com.example.demo.model.TaskPriority;
import com.example.demo.model.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskResponseDto(
        String name,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Instant dueDate,
        UUID assignedUser,
        UUID project
) {
}
