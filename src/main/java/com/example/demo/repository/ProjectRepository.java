package com.example.demo.repository;

import com.example.demo.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    @Modifying
    @Transactional
    @Query("DELETE FROM Project")
    void deleteAll();
}
