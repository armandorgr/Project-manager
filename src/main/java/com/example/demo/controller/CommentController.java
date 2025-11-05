package com.example.demo.controller;

import com.example.demo.controller.dto.CommentResponseDto;
import com.example.demo.controller.dto.CreateCommentDto;
import com.example.demo.controller.responses.ApiResponse;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.model.Comment;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.service.CommentService;
import com.example.demo.service.ProjectService;
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

@RestController
@RequestMapping("/api/project/{projectId}/tasks/{taskId}/comments")
public class CommentController {
    private final CommentService commentService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final CommentMapper commentMapper;

    public CommentController(
            CommentService commentService,
            ProjectService projectService,
            TaskService taskService,
            CommentMapper commentMapper
    ) {
        this.commentService = commentService;
        this.projectService = projectService;
        this.taskService = taskService;
        this.commentMapper = commentMapper;
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<CommentResponseDto>>> getComments(@PathVariable("projectId") UUID projectId, @PathVariable("taskId") UUID taskId) {
        Task task = this.taskService.getByIdAndProjectId(taskId, projectId);
        List<CommentResponseDto> comments = this.commentService.getAllCommentsByTask(task).stream().map(this.commentMapper::toResponse).toList();
        ApiResponse<List<CommentResponseDto>> response = new ApiResponse<>("SUCCESS", "Comments found", comments, null);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponseDto>> update(
            @RequestBody @Valid CreateCommentDto dto,
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId,
            @PathVariable("id") UUID commentId
    ) {
        Task task = this.taskService.getByIdAndProjectId(taskId, projectId);
        Comment comment = this.commentService.findCommentById(commentId);
        if (comment.getTask().getId() != task.getId()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment does not belong to task with given id");
        }
        this.commentMapper.updateFromDto(dto, comment);
        comment.setUpdatedAt(Instant.now());
        Comment savedComment = this.commentService.saveComment(comment);
        ApiResponse<CommentResponseDto> response = new ApiResponse<>("SUCCESS", "Comment updated", this.commentMapper.toResponse(savedComment), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping()
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
        ApiResponse<CommentResponseDto> response = new ApiResponse<>("SUCCESS", "Comment created", this.commentMapper.toResponse(savedComment), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponseDto>> delete(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId,
            @PathVariable("id") UUID commentId,
            Authentication auth
    ) {
        Task task = this.taskService.getByIdAndProjectId(taskId, projectId);
        Comment comment = this.commentService.findCommentById(commentId);
        if (comment.getTask().getId() != task.getId()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment does not belong to task with given id");
        }
        this.commentService.deleteComment(comment);
        ApiResponse<CommentResponseDto> response = new ApiResponse<>("SUCCESS", "Comment deleted", this.commentMapper.toResponse(comment), null);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
