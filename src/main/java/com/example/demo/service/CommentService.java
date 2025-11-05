package com.example.demo.service;

import com.example.demo.model.Comment;
import com.example.demo.model.Task;
import com.example.demo.repository.CommentRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository repository;

    public CommentService(CommentRepository repository){
        this.repository = repository;
    }

    public Comment saveComment(Comment comment){
        return repository.save(comment);
    }

    public Comment findCommentById(UUID commentId){
        return this.repository.findById(commentId).orElseThrow();
    }

    public void deleteComment(Comment comment){
        repository.delete(comment);
    }

    public void deleteComment(UUID commentId){
        repository.deleteById(commentId);
    }

    public List<Comment> getAllCommentsByTask(Task task){
        return repository.findAllByTask(task);
    }
}
