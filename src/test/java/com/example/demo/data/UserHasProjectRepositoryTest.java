package com.example.demo.data;

import com.example.demo.model.Project;
import com.example.demo.model.ProjectRole;
import com.example.demo.model.User;
import com.example.demo.model.UserHasProjects;
import com.example.demo.repository.UserHasProjectRepository;
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
public class UserHasProjectRepositoryTest {
    @Autowired
    UserHasProjectRepository repository;

    @Autowired
    TestEntityManager entityManager;
    //Se prueba que se puede asociar un usuario a un proyecto al cual pertenece
    @Test
    void givenUserAndProject_whenSave_thenSuccess(){
        User user = new User("test_user", "test_password", "email@gmail.com");
        Project project = new Project("Test Project", "Test description", Instant.now(), Instant.now().plusSeconds(10000));
        entityManager.persist(project);
        entityManager.persist(user);

        UserHasProjects userHasProjects = new UserHasProjects(user, project, ProjectRole.ADMIN);
        UserHasProjects insertedUserHasProjects = repository.save(userHasProjects);

        assertThat(entityManager.find(UserHasProjects.class, insertedUserHasProjects.getId())).isEqualTo(insertedUserHasProjects);
    }
    //Se prueba que se puede eliminar la relación existente entre un usuario y un proyecto, de modo que ya no forma parte de este.
    @Test
    void givenUserHasProject_whenDelete_thenSuccess(){
        User user = new User("test_user", "test_password", "email@gmail.com");
        Project project = new Project("Test Project", "Test description", Instant.now(), Instant.now().plusSeconds(10000));
        entityManager.persist(project);
        entityManager.persist(user);

        UserHasProjects userHasProjects = new UserHasProjects(user, project, ProjectRole.ADMIN);
        UserHasProjects insertedUserHasProjects = repository.save(userHasProjects);

        repository.deleteById(insertedUserHasProjects.getId());

        assertThat(entityManager.find(UserHasProjects.class, insertedUserHasProjects.getId())).isNull();
    }
}
