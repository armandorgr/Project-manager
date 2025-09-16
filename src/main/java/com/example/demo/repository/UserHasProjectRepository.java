package com.example.demo.repository;

import com.example.demo.model.User;
import com.example.demo.model.UserHasProjects;
import com.example.demo.model.UserProjectId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserHasProjectRepository extends JpaRepository<UserHasProjects, UserProjectId> {
    List<UserHasProjects> findAllByUser(User user);
}
