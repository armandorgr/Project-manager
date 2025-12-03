package com.example.demo.controller;

import com.example.demo.controller.anotations.projects.RequireProjectRole;
import com.example.demo.controller.dto.CommentResponseDto;
import com.example.demo.controller.dto.CreateCommentDto;
import com.example.demo.controller.responses.Response;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.model.Comment;
import com.example.demo.model.ProjectRole;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.service.CommentService;
import com.example.demo.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST controller responsible for managing comments associated with a specific task
 * within a project. Provides endpoints for listing, creating, updating and deleting comments.
 */
@Tag(name = "Comments", description = "Endpoints for managing comments within tasks in a project")
@RestController
@RequestMapping("/api/project/{projectId}/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final TaskService taskService;
    private final CommentMapper commentMapper;

    public CommentController(
            CommentService commentService,
            TaskService taskService,
            CommentMapper commentMapper
    ) {
        this.commentService = commentService;
        this.taskService = taskService;
        this.commentMapper = commentMapper;
    }

    /**
     * Retrieves all comments associated with a given task inside a project.
     *
     * @param projectId the UUID of the project
     * @param taskId    the UUID of the task
     * @return ResponseEntity containing a list of CommentResponseDto with HTTP 200 OK status
     */
    @Operation(
            summary = "Get all comments for a task",
            description = "Retrieves all comments associated with a given task inside a project.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of comments retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Task not found"
                    )
            }
    )
    @RequireProjectRole(ProjectRole.USER)
    @GetMapping
    public ResponseEntity<Response<List<CommentResponseDto>>> getComments(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId
    ) {
        Task task = this.taskService.getByIdAndProjectId(taskId, projectId);
        List<CommentResponseDto> comments = this.commentService
                .getAllCommentsByTask(task)
                .stream()
                .map(this.commentMapper::toResponse)
                .toList();

        Response<List<CommentResponseDto>> response =
                new Response<>("SUCCESS", "Comments found", comments, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing comment belonging to a specific task.
     *
     * @param dto       data transfer object containing the updated comment content
     * @param projectId UUID of the project
     * @param taskId    UUID of the task
     * @param commentId UUID of the comment to update
     * @return ResponseEntity containing the updated comment and HTTP 200 OK status
     * @throws ResponseStatusException if the comment does not belong to the specified task
     */
    @Operation(
            summary = "Update an existing comment",
            description = "Updates an existing comment belonging to a specific task.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Comment updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Comment does not belong to task or invalid request"
                    )
            }
    )
    @PatchMapping("/{id}")
    public ResponseEntity<Response<CommentResponseDto>> update(
            @RequestBody @Valid CreateCommentDto dto,
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId,
            @PathVariable("id") UUID commentId
    ) {
        Task task = this.taskService.getByIdAndProjectId(taskId, projectId);
        Comment comment = this.commentService.findCommentById(commentId);

        if (!comment.getTask().getId().equals(task.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Comment does not belong to task with given id");
        }

        this.commentMapper.updateFromDto(dto, comment);
        comment.setUpdatedAt(Instant.now());
        Comment savedComment = this.commentService.saveComment(comment);

        Response<CommentResponseDto> response =
                new Response<>("SUCCESS", "Comment updated", this.commentMapper.toResponse(savedComment), null);

        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new comment for a given task within a project.
     *
     * @param dto       data transfer object containing the new comment content
     * @param projectId UUID of the project
     * @param taskId    UUID of the task
     * @param auth      Authentication object representing the current user
     * @return ResponseEntity containing the created comment with HTTP 201 CREATED status
     */
    @Operation(
            summary = "Create a new comment",
            description = "Creates a new comment for a given task within a project.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Comment created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request or task not found"
                    )
            }
    )
    @RequireProjectRole(ProjectRole.USER)
    @PostMapping
    public ResponseEntity<Response<CommentResponseDto>> create(
            @RequestBody @Valid CreateCommentDto dto,
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId,
            Authentication auth
    ) {
        Task task = this.taskService.getByIdAndProjectId(taskId, projectId);
        User currentUser = (User) auth.getPrincipal();

        Comment comment = this.commentMapper.toEntity(dto);
        comment.setUser(currentUser);
        comment.setTask(task);

        Comment savedComment = this.commentService.saveComment(comment);
        Response<CommentResponseDto> response =
                new Response<>("SUCCESS", "Comment created",
                        this.commentMapper.toResponse(savedComment), null);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Deletes a comment associated with a given task.
     *
     * @param projectId UUID of the project
     * @param taskId    UUID of the task
     * @param commentId UUID of the comment to delete
     * @param auth      Authentication object representing the current user
     * @return ResponseEntity with HTTP 204 NO_CONTENT if deletion was successful
     * @throws ResponseStatusException if the comment does not belong to the specified task
     */
    @Operation(
            summary = "Delete a comment",
            description = "Deletes a comment associated with a given task.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Comment deleted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Comment does not belong to task"
                    )
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId,
            @PathVariable("id") UUID commentId,
            Authentication auth
    ) {
        Task task = this.taskService.getByIdAndProjectId(taskId, projectId);
        Comment comment = this.commentService.findCommentById(commentId);

        if (!comment.getTask().getId().equals(task.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Comment does not belong to task with given id");
        }

        this.commentService.deleteComment(comment);
        return ResponseEntity.noContent().build();
    }
}
