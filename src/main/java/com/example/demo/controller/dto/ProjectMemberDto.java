package com.example.demo.controller.dto;

import com.example.demo.model.ProjectRole;

public record ProjectMemberDto(
        UserResponseDto user,
        ProjectRole role
) {
}
