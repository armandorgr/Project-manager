package com.example.demo.controller;

import com.example.demo.controller.anotations.projects.RequireProjectRole;
import com.example.demo.controller.dto.CreateTaskDto;
import com.example.demo.controller.dto.TaskResponseDto;
import com.example.demo.controller.dto.UpdateTaskDto;
import com.example.demo.controller.responses.Response;
import com.example.demo.mapper.TaskMapper;
import com.example.demo.model.*;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.ProjectService;
import com.example.demo.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Tasks", description = "Endpoints to manage tasks within projects")
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
    @Operation(
            summary = "Get all tasks assigned to the authenticated user",
            description = "Retrieves all tasks assigned to the currently authenticated user.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Assigned tasks retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @GetMapping("/assigned")
    public ResponseEntity<Response<List<TaskResponseDto>>> getCurrentUserTasks(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<TaskResponseDto> assignedTasks = this.taskService
                .getAllTasksByUser(currentUser)
                .stream()
                .map(taskMapper::toResponse)
                .toList();

        Response<List<TaskResponseDto>> response =
                new Response<>("SUCCESS", "Assigned tasks", assignedTasks, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new task within the specified project.
     *
     * @param dto       DTO containing task creation data
     * @param projectId UUID of the project where the task will be created
     * @return ResponseEntity containing created task and HTTP 201 CREATED
     */
    @Operation(
            summary = "Create a new task in a project",
            description = "Creates a new task within the specified project.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Task created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Project not found")
            }
    )
    @RequireProjectRole(ProjectRole.ADMIN)
    @Transactional
    @PostMapping
    public ResponseEntity<Response<TaskResponseDto>> createTask(
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
        Response<TaskResponseDto> response =
                new Response<>("SUCCESS", "Task created", taskMapper.toResponse(savedTask), null);
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
    @Operation(
            summary = "Update an existing task",
            description = "Updates an existing task within a project.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Task updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Task or project not found")
            }
    )
    @RequireProjectRole(ProjectRole.ADMIN)
    @Transactional
    @PatchMapping("/{taskId}")
    public ResponseEntity<Response<TaskResponseDto>> updateTask(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId,
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
        Response<TaskResponseDto> response =
                new Response<>("SUCCESS", "Task updated successfully", this.taskMapper.toResponse(updatedTask), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a task from the specified project.
     *
     * @param taskId    UUID of the task to delete
     * @param projectId UUID of the project containing the task
     * @return ResponseEntity with HTTP 204 NO_CONTENT if deleted successfully
     */
    @Operation(
            summary = "Delete a task from a project",
            description = "Deletes a task from the specified project.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
                    @ApiResponse(responseCode = "403", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Task or project not found")
            }
    )
    @RequireProjectRole(ProjectRole.ADMIN)
    @Transactional
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId
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
    @Operation(
            summary = "Get all tasks of a project",
            description = "Retrieves all tasks belonging to a specific project.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Project tasks retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Project not found")
            }
    )
    @RequireProjectRole(ProjectRole.USER)
    @GetMapping
    public ResponseEntity<Response<List<TaskResponseDto>>> getAllTasksByProject(
            @PathVariable("projectId") UUID projectId
    ) {
        Project project = this.projectService.getOneById(projectId);
        List<TaskResponseDto> tasks = this.projectService
                .getAllTasksByProject(project)
                .stream()
                .map(this.taskMapper::toResponse)
                .toList();

        Response<List<TaskResponseDto>> response =
                new Response<>("SUCCESS", "Project tasks", tasks, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all tasks belonging to a specific project.
     *
     * @param id UUID of the task
     * @return ResponseEntity containing the list of project tasks and HTTP 200 OK
     */
    @Operation(
            summary = "Get one task of a project",
            description = "Retrieves one tasks belonging to a specific project.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Project task retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Task not found")
            }
    )
    @RequireProjectRole(ProjectRole.USER)
    @GetMapping("/{taskId}")
    public ResponseEntity<Response<TaskResponseDto>> getById(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID id
    ) {
        Task task = this.taskService.findById(id);
        Response<TaskResponseDto> response = new Response<>("SUCCESS", "Task found", this.taskMapper.toResponse(task), null);
        return ResponseEntity.ok(response);
    }
}
