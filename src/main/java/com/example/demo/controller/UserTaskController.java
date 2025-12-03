package com.example.demo.controller;

import com.example.demo.controller.dto.TaskResponseDto;
import com.example.demo.controller.responses.Response;
import com.example.demo.mapper.TaskMapper;
import com.example.demo.model.User;
import com.example.demo.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "User Tasks", description = "Endpoints for user-specific task operations")
@RestController
@RequestMapping("/api/tasks")
public class UserTaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;

    public UserTaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @Operation(summary = "Get all tasks assigned to the authenticated user")
    @GetMapping("/assigned") // 2. Resultado final: /api/tasks/assigned
    public ResponseEntity<Response<List<TaskResponseDto>>> getCurrentUserTasks(@AuthenticationPrincipal User currentUser) {
        List<TaskResponseDto> assignedTasks = this.taskService
                .getAllTasksByUser(currentUser)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
        return ResponseEntity.ok(new Response<>("SUCCESS", "Assigned tasks", assignedTasks, null));
    }
}