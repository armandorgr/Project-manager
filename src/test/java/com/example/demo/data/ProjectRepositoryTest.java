package com.example.demo.data;

import com.example.demo.model.Project;
import com.example.demo.repository.ProjectRepository;
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
public class ProjectRepositoryTest {
    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void givenNewProject_whenSave_thenSuccess() {
        Project newProject = new Project("Test Project", "Test description", Instant.now(), Instant.now().plusSeconds(10000));
        Project insertedProject = projectRepository.save(newProject);
        assertThat(entityManager.find(Project.class, insertedProject.getId())).isEqualTo(newProject);
    }
    //Se prueba que se puede actualizar la informacion de un proyecto
    @Test
    void givenNewProject_whenSaveAndUpdated_thenSuccess(){
        Project newProject = new Project("Test Project", "Test description", Instant.now(), Instant.now().plusSeconds(10000));
        Project insertedProject = projectRepository.save(newProject);
        assertThat(entityManager.find(Project.class, insertedProject.getId())).isEqualTo(newProject);

        //Updates project name ,description and end date
        String newName = "new name";
        String newDescription = "new description";
        Instant newEndDate = insertedProject.getEndDate().plusSeconds(60000);

        insertedProject.setName(newName);
        insertedProject.setDescription(newDescription);
        insertedProject.setEndDate(newEndDate);

        Project updatedProject = projectRepository.save(insertedProject);

        assertThat(updatedProject.getId()).isEqualTo(insertedProject.getId());
        assertThat(updatedProject.getName()).isEqualTo(newName);
        assertThat(updatedProject.getDescription()).isEqualTo(newDescription);
        assertThat(updatedProject.getEndDate()).isEqualTo(newEndDate);
    }

    @Test
    void givenProjectId_whenDelete_thenSuccess(){
        Project newProject = new Project("Test Project", "Test description", Instant.now(), Instant.now().plusSeconds(10000));
        Project insertedProject = projectRepository.save(newProject);
        assertThat(entityManager.find(Project.class, insertedProject.getId())).isEqualTo(newProject);

        //delete it
        projectRepository.deleteById(insertedProject.getId());
        assertThat(entityManager.find(Project.class, insertedProject.getId())).isNull();
    }
}
