package com.example.demo.data;

import com.example.demo.model.*;
import com.example.demo.repository.CommentRepository;
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
public class CommentRepositoryTest {
    @Autowired
    CommentRepository commentRepository;

    @Autowired
    TestEntityManager entityManager;
    //Dado un proyecto, con una tarea, se prueba que se guarda un comentario en dicha tarea correctamente
    @Test
    void givenNewComment_whenSave_thenSuccess() {
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
        entityManager.persist(newTask);

        Comment newComment = new Comment("new comment", Instant.now(), user, newTask);
        commentRepository.save(newComment);

        assertThat(entityManager.find(Comment.class, newComment.getId())).isEqualTo(newComment);
    }
    //Se prueba que se puede modificar el mensaje de un comentario despues de haberlo creado.
    @Test
    void givenComment_whenUpdate_thenSuccess() {
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
        entityManager.persist(newTask);

        Comment newComment = new Comment("new comment", Instant.now(),user, newTask);
        Comment insertedComment = entityManager.persist(newComment);

        String newContent = "Nuevo contenido";
        insertedComment.setContent(newContent);

        commentRepository.save(insertedComment);

        assertThat(entityManager.find(Comment.class, insertedComment.getId()).getContent()).isEqualTo(newContent);
    }
    //Se prueba que se puede eliminar un comentario.
    @Test
    void givenComment_whenDelete_thenSuccess() {
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
        entityManager.persist(newTask);

        Comment newComment = new Comment("new comment", Instant.now() ,user, newTask);
        Comment insertedComment = entityManager.persist(newComment);

        commentRepository.deleteById(insertedComment.getId());

        assertThat(entityManager.find(Comment.class, insertedComment.getId())).isNull();
    }
}
