package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserHasProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {
    private final TaskRepository taskRepository;
    private final ProjectRepository repository;
    private final UserHasProjectRepository userHasProjectRepository;

    public ProjectService(ProjectRepository repository, UserHasProjectRepository userHasProjectRepository, TaskRepository taskRepository) {
        this.repository = repository;
        this.taskRepository = taskRepository;
        this.userHasProjectRepository = userHasProjectRepository;
    }

    public UserHasProjects addUserToProject(User user, Project project, ProjectRole role) {
        return userHasProjectRepository.save(new UserHasProjects(user, project, role));
    }

    //Deletes relation between user and project, and unassign the tasks they had.
    public void kickUserFromProject(User user, Project project) {
        userHasProjectRepository.findById(new UserProjectId(user.getId(), project.getId())).ifPresent((i) -> {
            userHasProjectRepository.delete(i);
            List<Task> assignedTasks = taskRepository.findAllByUserAndProject(user, project);
            for (Task t : assignedTasks) {
                t.setUser(null); //unassign user to tasks they had
            }
            taskRepository.saveAll(assignedTasks);
        });

    }

    //Get all projects a user is part of
    public List<Project> getAllProjectsByUser(User user) {
        return userHasProjectRepository.findAllByUser(user)
                .stream()
                .map(UserHasProjects::getProject)
                .toList();
    }

    public Project saveProject(Project project) {
        return repository.save(project);
    }

    public void deleteProject(UUID projectId) {
        repository.deleteById(projectId);
    }

    public void deleteProject(Project project) {
        repository.delete(project);
    }
}
