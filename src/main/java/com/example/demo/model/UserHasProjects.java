package com.example.demo.model;

import jakarta.persistence.*;


@Entity
@Table(name = "user_has_projects")
public class UserHasProjects {

    @EmbeddedId
    private UserProjectId id = new UserProjectId();

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("projectId")
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column
    private ProjectRole role;

    public UserHasProjects(User user, Project project, ProjectRole role) {
        this.user = user;
        this.project = project;
        this.role = role;
    }

    public UserHasProjects(){}

    public UserProjectId getId() {
        return id;
    }

    public void setId(UserProjectId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public ProjectRole getRole() {
        return role;
    }

    public void setRole(ProjectRole role) {
        this.role = role;
    }
}

