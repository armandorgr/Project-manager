package com.example.demo.controller;

import com.example.demo.controller.anotations.projects.RequireProjectRole;
import com.example.demo.controller.dto.CommentResponseDto;
import com.example.demo.controller.dto.CreateCommentDto;
import com.example.demo.controller.responses.ApiResponse;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.model.Comment;
import com.example.demo.model.ProjectRole;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.service.CommentService;
import com.example.demo.service.TaskService;
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
    @RequireProjectRole(ProjectRole.USER)
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponseDto>>> getComments(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId
    ) {
        Task task = this.taskService.getByIdAndProjectId(taskId, projectId);
        List<CommentResponseDto> comments = this.commentService
                .getAllCommentsByTask(task)
                .stream()
                .map(this.commentMapper::toResponse)
                .toList();

        ApiResponse<List<CommentResponseDto>> response =
                new ApiResponse<>("SUCCESS", "Comments found", comments, null);
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
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponseDto>> update(
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

        ApiResponse<CommentResponseDto> response =
                new ApiResponse<>("SUCCESS", "Comment updated", this.commentMapper.toResponse(savedComment), null);

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
    @RequireProjectRole(ProjectRole.USER)
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponseDto>> create(
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
        ApiResponse<CommentResponseDto> response =
                new ApiResponse<>("SUCCESS", "Comment created",
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
