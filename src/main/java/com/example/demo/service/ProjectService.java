package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserHasProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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

    public UserHasProjects addUserToProject(UserHasProjects relation) {
        return userHasProjectRepository.save(relation);
    }

    //Deletes relation between user and project, and unassign the tasks they had.
    public void kickUserFromProject(User user, Project project) {
        userHasProjectRepository.findById(new UserProjectId(user.getId(), project.getId())).ifPresent(userHasProjectRepository::delete);

    }

    public UserHasProjects getRelation(UserProjectId relationId){
        return userHasProjectRepository.findById(relationId).orElseThrow();
    }

    //Get all projects a user is part of
    public List<Project> getAllProjectsByUser(UUID userId) {
        return userHasProjectRepository.findProjectsByUserId(userId);
    }

    public Project getOneById(UUID projectId)throws NoSuchElementException {
        return this.repository.findById(projectId).orElseThrow();
    }

    public List<Project> getOneByQuery(String query, UUID userId){
        return this.userHasProjectRepository.findProjectsByNameOrDescription(query, userId);
    }

    public List<Task> getAllTasksByProject(Project project){
        return this.taskRepository.findAllByProject(project);
    }

    public Task saveTask(Task task){
        return this.taskRepository.save(task);
    }

    public Task findTaskById(UUID taskId, UUID projectId){
        return this.taskRepository.findByIdAndProjectId(taskId, projectId).orElseThrow();
    }

    public Optional<UserHasProjects> getRelationSafe(UUID userId, UUID projectId) {
        try {
            return Optional.of(getRelation(new UserProjectId(userId, projectId)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<UserHasProjects> getMembers(UUID projectId){
        return this.userHasProjectRepository.findAllByProject(projectId);
    }

    public Project saveProject(Project project) {
        return repository.save(project);
    }

    @Transactional
    public void deleteProject(Project project) {
        this.userHasProjectRepository.deleteAllByProjectId(project.getId());
        this.repository.deleteById(project.getId());
    }

    @Transactional
    public void deleteTask(Task task){
        this.taskRepository.delete(task);
    }

    @Transactional
    public void deleteTask(UUID taskId){
        this.taskRepository.deleteById(taskId);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}
