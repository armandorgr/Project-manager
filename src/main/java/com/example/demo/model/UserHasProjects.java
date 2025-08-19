package com.example.demo.model;

import jakarta.persistence.*;


@Entity
@Table(name = "user_has_projects")
public class UserHasProjects {

    @EmbeddedId
    private UserProjectId id;

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
}

