package com.example.demo.repository;

import com.example.demo.model.UserHasProjects;
import com.example.demo.model.UserProjectId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHasProjectRepository extends JpaRepository<UserHasProjects, UserProjectId> {
}
