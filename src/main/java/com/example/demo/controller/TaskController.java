package com.example.demo.controller;

import com.example.demo.controller.anotations.projects.RequireProjectRole;
import com.example.demo.controller.dto.CreateTaskDto;
import com.example.demo.controller.dto.TaskResponseDto;
import com.example.demo.controller.dto.UpdateTaskDto;
import com.example.demo.controller.responses.ApiResponse;
import com.example.demo.mapper.TaskMapper;
import com.example.demo.model.*;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.ProjectService;
import com.example.demo.service.TaskService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller responsible for managing tasks within a specific project.
 * Provides endpoints to create, update, delete and list tasks, as well as retrieve
 * tasks assigned to the current authenticated user.
 */
@RestController
@RequestMapping("/api/project/{projectId}/tasks")
public class TaskController {

    private final Logger logger = LoggerFactory.getLogger(TaskController.class);
    private final TaskMapper taskMapper;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final CustomUserDetailsService userService;

    public TaskController(
            TaskMapper taskMapper,
            TaskService taskService,
            CustomUserDetailsService userService,
            ProjectService projectService
    ) {
        this.taskMapper = taskMapper;
        this.taskService = taskService;
        this.userService = userService;
        this.projectService = projectService;
    }

    /**
     * Retrieves all tasks assigned to the currently authenticated user.
     *
     * @param authentication Authentication object containing the current user
     * @return ResponseEntity containing a list of tasks and HTTP 200 OK
     */
    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse<List<TaskResponseDto>>> getCurrentUserTasks(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<TaskResponseDto> assignedTasks = this.taskService
                .getAllTasksByUser(currentUser)
                .stream()
                .map(taskMapper::toResponse)
                .toList();

        ApiResponse<List<TaskResponseDto>> response =
                new ApiResponse<>("SUCCESS", "Assigned tasks", assignedTasks, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new task within the specified project.
     *
     * @param dto       DTO containing task creation data
     * @param projectId UUID of the project where the task will be created
     * @return ResponseEntity containing created task and HTTP 201 CREATED
     */
    @RequireProjectRole(ProjectRole.ADMIN)
    @Transactional
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponseDto>> createTask(
            @RequestBody @Valid CreateTaskDto dto,
            @PathVariable("projectId") UUID projectId
    ) {
        Project project = this.projectService.getOneById(projectId);
        Task task = taskMapper.toEntity(dto);
        task.setProject(project);

        if (dto.userId() != null) {
            User user = (User) this.userService.loadUserById(dto.userId());
            task.setUser(user);
        }

        Task savedTask = this.projectService.saveTask(task);
        ApiResponse<TaskResponseDto> response =
                new ApiResponse<>("SUCCESS", "Task created", taskMapper.toResponse(savedTask), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing task within a project.
     *
     * @param taskId    UUID of the task to update
     * @param projectId UUID of the project containing the task
     * @param dto       DTO containing updated task fields
     * @return ResponseEntity with updated task and HTTP 200 OK
     */
    @RequireProjectRole(ProjectRole.ADMIN)
    @Transactional
    @PatchMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponseDto>> updateTask(
            @PathVariable("taskId") UUID taskId,
            @PathVariable("projectId") UUID projectId,
            @RequestBody UpdateTaskDto dto
    ) {
        Task task = this.projectService.findTaskById(taskId, projectId);
        this.taskMapper.updateTaskFromDto(dto, task);

        if (dto.assignedUser() != null) {
            User newAssignedUser = this.projectService
                    .getRelation(new UserProjectId(dto.assignedUser(), task.getProject().getId()))
                    .getUser();
            task.setUser(newAssignedUser);
        }

        Task updatedTask = this.projectService.saveTask(task);
        ApiResponse<TaskResponseDto> response =
                new ApiResponse<>("SUCCESS", "Task updated successfully", this.taskMapper.toResponse(updatedTask), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a task from the specified project.
     *
     * @param taskId    UUID of the task to delete
     * @param projectId UUID of the project containing the task
     * @return ResponseEntity with HTTP 204 NO_CONTENT if deleted successfully
     */
    @RequireProjectRole(ProjectRole.ADMIN)
    @Transactional
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable("taskId") UUID taskId,
            @PathVariable("projectId") UUID projectId
    ) {
        Task task = this.projectService.findTaskById(taskId, projectId);
        this.projectService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all tasks belonging to a specific project.
     *
     * @param projectId UUID of the project
     * @return ResponseEntity containing the list of project tasks and HTTP 200 OK
     */
    @RequireProjectRole(ProjectRole.USER)
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponseDto>>> getAllTasksByProject(
            @PathVariable("projectId") UUID projectId
    ) {
        Project project = this.projectService.getOneById(projectId);
        List<TaskResponseDto> tasks = this.projectService
                .getAllTasksByProject(project)
                .stream()
                .map(this.taskMapper::toResponse)
                .toList();

        ApiResponse<List<TaskResponseDto>> response =
                new ApiResponse<>("SUCCESS", "Project tasks", tasks, null);
        return ResponseEntity.ok(response);
    }
}
