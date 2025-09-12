package com.example.demo.data;

import com.example.demo.model.*;
import com.example.demo.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class TaskRepositoryTest {
    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void givenNewTask_whenSave_thenSuccess() {
        User user = new User("test_user", "test_password", "email@gmail.com");
        Project project = new Project("Test Project", "Test description", Instant.now(), Instant.now().plusSeconds(10000));

        entityManager.persist(project);
        entityManager.persist(user);

        Task newTask = new Task(
                "tarea",
                "descripcion",
                TaskStatus.NOT_STARTED,
                TaskPriority.CRITICAL,
                Instant.now().plusSeconds(60),
                user,
                project
        );

        Task insertedTask = taskRepository.save(newTask);

        assertThat(entityManager.find(Task.class, newTask.getId())).isEqualTo(insertedTask);
    }

    @Test
    void givenNewTask_whenSaveThenUpdate_thenSuccess(){
        User user = new User("test_user", "test_password", "email@gmail.com");
        Project project = new Project("Test Project", "Test description", Instant.now(), Instant.now().plusSeconds(10000));

        entityManager.persist(project);
        entityManager.persist(user);

        Task newTask = new Task(
                "tarea",
                "descripcion",
                TaskStatus.NOT_STARTED,
                TaskPriority.CRITICAL,
                Instant.now().plusSeconds(60),
                user,
                project
        );
        Task insertedTask = entityManager.persist(newTask);
        //Update name, description, status, and user
        String newName = "new name";
        String newDescription = "new Description";
        TaskStatus newStatus = TaskStatus.DONE;
        User newUser = new User("test_user_2", "test_passwor", "email_2@gmail.com");
        entityManager.persist(newUser);

        insertedTask.setName(newName);
        insertedTask.setDescription(newDescription);
        insertedTask.setStatus(newStatus);
        insertedTask.setUser(newUser);
        //saves
        taskRepository.save(insertedTask);
        Task foundTask = entityManager.find(Task.class, insertedTask.getId());

        assertThat(foundTask.getName()).isEqualTo(newName);
        assertThat(foundTask.getDescription()).isEqualTo(newDescription);
        assertThat(foundTask.getStatus()).isEqualTo(newStatus);
        assertThat(foundTask.getUser()).isEqualTo(newUser);
    }
}
