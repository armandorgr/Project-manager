package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_has_user")
public class UserHasUser {
    @EmbeddedId
    private UserUserId id;

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
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

    @Column
    private String message;
}
