package com.example.demo.repository;

import com.example.demo.model.Project;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findByIdAndProjectId(UUID taskId, UUID projectId);
    List<Task> findAllByProject(Project project);
    List<Task> findAllByUser(User user);
    List<Task> findAllByUserAndProject(User user, Project project);
    @Modifying
    @Transactional
    @Query("DELETE FROM Task")
    void deleteAll();
}
