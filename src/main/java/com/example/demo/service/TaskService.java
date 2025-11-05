package com.example.demo.service;

import com.example.demo.model.Project;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.repository.TaskRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository){
        this.taskRepository = taskRepository;
    }

    public List<Task> getAllTasksByUser(User user){
        return taskRepository.findAllByUser(user);
    }

    public Task getByIdAndProjectId(UUID taskId, UUID projectId){
        return this.taskRepository.findByIdAndProjectId(taskId, projectId).orElseThrow();
    }

    public Task findById(UUID taskId){
        return this.taskRepository.findById(taskId).orElseThrow();
    }

    public Task saveTask(Task task){
        return taskRepository.save(task);
    }

    public void deleteTask(Task task){
        taskRepository.delete(task);
    }

    public void deleteTask(UUID taskId){
        taskRepository.deleteById(taskId);
    }
}
