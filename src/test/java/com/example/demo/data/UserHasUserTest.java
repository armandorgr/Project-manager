package com.example.demo.data;

import com.example.demo.model.Project;
import com.example.demo.model.User;
import com.example.demo.model.UserHasUser;
import com.example.demo.repository.UserHasUserRepository;
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
public class UserHasUserTest {
    @Autowired
    UserHasUserRepository repository;

    @Autowired
    TestEntityManager entityManager;
    //Se prueba que se puede crear una relación entre dos usuarios, la cual representa una invitación de uno al otro para unirse a un proyecto.
    @Test
    void givenUsers_whenSave_thenSuccess(){
        User user = new User("test_user", "test_password", "email@gmail.com");
        User user2 = new User("test_user_2", "test_password", "email2@gmail.com");
        Project project = new Project("Test Project", "Test description", Instant.now(), Instant.now().plusSeconds(10000));
        entityManager.persist(user2);
        entityManager.persist(project);
        entityManager.persist(user);

        UserHasUser userHasUser = new UserHasUser(user, user2, project, "mensaje");
        UserHasUser insertedEntity = repository.save(userHasUser);

        assertThat(entityManager.find(UserHasUser.class, insertedEntity.getId())).isEqualTo(insertedEntity);
    }

    @Test
    void given_UserHasUser_whenDelete_thenSuccess(){
        User user = new User("test_user", "test_password", "email@gmail.com");
        User user2 = new User("test_user_2", "test_password", "email2@gmail.com");
        Project project = new Project("Test Project", "Test description", Instant.now(), Instant.now().plusSeconds(10000));
        entityManager.persist(user2);
        entityManager.persist(project);
        entityManager.persist(user);

        UserHasUser userHasUser = new UserHasUser(user, user2, project, "mensaje");
        UserHasUser insertedEntity = entityManager.persist(userHasUser);
        repository.findById(insertedEntity.getId()).ifPresent((u) -> {
            repository.delete(u);
        });
        assertThat(entityManager.find(UserHasUser.class, insertedEntity.getId())).isNull();
    }
}
