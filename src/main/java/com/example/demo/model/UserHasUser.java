package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_has_user")
public class UserHasUser {
    @EmbeddedId
    private UserUserId id = new UserUserId();

    @ManyToOne
    @MapsId("senderId")
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @MapsId("receiverId")
    @JoinColumn(name = "receiver_id")
    private User user;

    @ManyToOne
    @MapsId("projectId")
    @JoinColumn(name = "project_id")
    private Project project;

    @Column
    private String message;

    public UserHasUser(User sender, User receiver, Project project, String message) {
        this.sender = sender;
        this.user = receiver;
        this.project = project;
        this.message = message;
    }

    public UserHasUser() {
    }

    public UserUserId getId() {
        return id;
    }

    public void setId(UserUserId id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
