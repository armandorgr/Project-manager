package com.example.demo.controller;

import com.example.demo.controller.dto.CreateTaskDto;
import com.example.demo.controller.dto.TaskResponseDto;
import com.example.demo.controller.dto.UpdateTaskDto;
import com.example.demo.controller.responses.ApiResponse;
import com.example.demo.mapper.TaskMapper;
import com.example.demo.model.Project;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.model.UserProjectId;
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

@RestController
@RequestMapping("/api/project/{projectId}/tasks")
public class TaskController {
    private final Logger logger = LoggerFactory.getLogger(TaskController.class);
    private final TaskMapper taskMapper;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final CustomUserDetailsService userService;

    public TaskController(TaskMapper taskMapper, TaskService taskService, CustomUserDetailsService userService, ProjectService projectService) {
        this.taskMapper = taskMapper;
        this.taskService = taskService;
        this.userService = userService;
        this.projectService = projectService;
    }

    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse<List<TaskResponseDto>>> getCurrentUserTasks(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<TaskResponseDto> assignedTasks = this.taskService.getAllTasksByUser(currentUser).stream().map(taskMapper::toResponse).toList();
        ApiResponse<List<TaskResponseDto>> response = new ApiResponse<>("SUCCESS", "Assigned tasks", assignedTasks, null);
        return ResponseEntity.ok(response);
    }

    @Transactional
    @PostMapping()
    public ResponseEntity<ApiResponse<TaskResponseDto>> createTask(@RequestBody @Valid CreateTaskDto dto, @PathVariable("projectId") UUID projectId) {
        // Find project
        Project project = this.projectService.getOneById(projectId);
        Task task = taskMapper.toEntity(dto);
        task.setProject(project);

        if (dto.userId() != null) {
            // Find user
            User user = (User) this.userService.loadUserById(dto.userId());
            task.setUser(user);
        }
        Task savedTask = this.projectService.saveTask(task);
        ApiResponse<TaskResponseDto> response = new ApiResponse<>("SUCCESS", "Task created", taskMapper.toResponse(savedTask), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Transactional
    @PatchMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponseDto>> updateTask(@PathVariable("taskId") UUID taskId, @PathVariable("projectId") UUID projectId, @RequestBody UpdateTaskDto dto) {
        // Find task to update
        Task task = this.projectService.findTaskById(taskId, projectId); // If not found task doesn't belong to project or doesn't exist at all
        this.taskMapper.updateTaskFromDto(dto, task);
        if (dto.assignedUser() != null) {
            User newAssignedUser = this.projectService.getRelation(new UserProjectId(dto.assignedUser(), task.getProject().getId())).getUser();
            task.setUser(newAssignedUser);
        }
        Task updatedTask = this.projectService.saveTask(task);
        ApiResponse<TaskResponseDto> response = new ApiResponse<>("SUCCESS", "Task updated successfully", this.taskMapper.toResponse(updatedTask), null);
        return ResponseEntity.ok(response);
    }

    @Transactional
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Object>> deleteTask(@PathVariable("taskId") UUID taskId, @PathVariable("projectId") UUID projectId) {
        Task task = this.projectService.findTaskById(taskId, projectId);
        this.projectService.deleteTask(taskId);
        return ResponseEntity.ok(new ApiResponse<>("SUCEESS", "Task deleted successfully", null, null));
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<TaskResponseDto>>> getAllTasksByProject(@PathVariable("projectId") UUID projectId) {
        Project project = this.projectService.getOneById(projectId);
        List<TaskResponseDto> tasks = this.projectService.getAllTasksByProject(project).stream().map(this.taskMapper::toResponse).toList();
        ApiResponse<List<TaskResponseDto>> response = new ApiResponse<>("SUCCESS", "Project tasks", tasks, null);
        return ResponseEntity.ok(response);
    }
}
