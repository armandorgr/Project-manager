package com.example.demo.repository;

import com.example.demo.model.Project;
import com.example.demo.model.User;
import com.example.demo.model.UserHasProjects;
import com.example.demo.model.UserProjectId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserHasProjectRepository extends JpaRepository<UserHasProjects, UserProjectId> {
    @Query("SELECT up.project FROM UserHasProjects up WHERE up.user.id = :userId")
    List<Project> findProjectsByUserId(@Param("userId") UUID userId);

    @Query("SELECT up FROM UserHasProjects up WHERE up.project.id = :projectId")
    List<UserHasProjects> findAllByProject(@Param("projectId") UUID projectId);

    @Query("SELECT up.project FROM UserHasProjects up WHERE up.user.id = :userId AND LOWER(up.project.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(up.project.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Project> findProjectsByNameOrDescription(@Param("keyword") String keyword, @Param("userId") UUID userId);

    void deleteAllByProjectId(UUID projectId);
}
