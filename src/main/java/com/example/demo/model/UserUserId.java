package com.example.demo.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserUserId implements Serializable {
    private UUID senderId;
    private UUID receiverId;
    private UUID projectId;

    public UserUserId(){}

    public UserUserId(UUID senderId, UUID receiverId, UUID projectId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.projectId = projectId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }

    public UUID getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(UUID receiverId) {
        this.receiverId = receiverId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserUserId that)) return false;
        return Objects.equals(senderId, that.senderId) && Objects.equals(receiverId, that.receiverId) && Objects.equals(projectId, that.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderId, receiverId, projectId);
    }
}
