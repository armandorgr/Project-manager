package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/dev/seed")
@Profile("dev")
public class SeedController {

    private final CustomUserDetailsService userService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final InvitationService invitationService;
    private final CommentService commentService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;


    public SeedController(CustomUserDetailsService userService, ProjectService projectService, TaskService taskService, InvitationService invitationService, CommentService commentService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.projectService = projectService;
        this.taskService = taskService;
        this.invitationService = invitationService;
        this.commentService = commentService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<String> seed() {

        if (!seedEnabled) {
            return ResponseEntity.status(403).body("Seed disabled");
        }

        // -------------------------
        // CLEAN DATABASE
        // -------------------------
        invitationService.deleteAll();
        taskService.deleteAll();
        projectService.deleteAll();
        userService.deleteAll();

        // -------------------------
        // CREATE USERS
        // -------------------------
        User admin = (User) userService.registerUser("admin",passwordEncoder.encode("admin123"),"admin@mail.com");
        User user = (User) userService.registerUser("user", passwordEncoder.encode("user123"),"user@mail.com");

        // -------------------------
        // CREATE PROJECTS
        // -------------------------
        Project p1 = new Project("Project Alpha", "Main platform development", Instant.now(), Instant.now());
        Project p2 = new Project("Project Beta", "Marketing campaign", Instant.now(), null);
        Project p3 = new Project("Project Gamma", "Internal tools", Instant.now(), null);

        p1 = projectService.saveProject(p1);
        p2 = projectService.saveProject(p2);
        p3 = projectService.saveProject(p3);

        projectService.addUserToProject(new UserHasProjects(admin, p1, ProjectRole.ADMIN));
        projectService.addUserToProject(new UserHasProjects(admin, p2, ProjectRole.ADMIN));
        projectService.addUserToProject(new UserHasProjects(admin, p3, ProjectRole.ADMIN));

        projectService.addUserToProject(new UserHasProjects(user, p1, ProjectRole.USER));

        // -------------------------
        // CREATE TASKS
        // -------------------------
        Random random = new Random();

        createTasksForProject(p1, admin, user, random);
        createTasksForProject(p2, admin, user, random);
        createTasksForProject(p3, admin, user, random);

        // -------------------------
        // CREATE INVITATIONS
        // -------------------------
        UserHasUser inv1 = new UserHasUser(
                admin,
                user,
                p2,
                "Admin invites user to Project Beta"
        );
        invitationService.sendInvitation(inv1);

        UserHasUser inv2 = new UserHasUser(
                user,
                admin,
                p3,
                "User invites admin to Project Gamma"
        );
        invitationService.sendInvitation(inv2);

        return ResponseEntity.ok("Seed executed successfully!");
    }

    private void createTasksForProject(Project project, User admin, User user, Random random) {

        int taskCount = 5 + random.nextInt(6); // from 5 to 10 tasks

        List<TaskStatus> statuses = List.of(TaskStatus.NOT_STARTED, TaskStatus.IN_PROGRESS, TaskStatus.DONE);
        List<TaskPriority> priorities = List.of(TaskPriority.LOW, TaskPriority.MEDIUM, TaskPriority.HIGH);

        for (int i = 0; i < taskCount; i++) {

            Task task = new Task();
            task.setName("Task " + (i + 1) + " for " + project.getName());
            task.setDescription("Auto-generated task " + (i + 1));
            task.setStatus(statuses.get(random.nextInt(statuses.size())));
            task.setPriority(priorities.get(random.nextInt(priorities.size())));
            task.setDueDate(Instant.now().plusSeconds(86400L * (1 + random.nextInt(10))));
            task.setProject(project);

            // assign some tasks to admin or user randomly
            if (random.nextBoolean()) {
                task.setUser(random.nextBoolean() ? admin : user);
            }

            Task saved = taskService.saveTask(task);

            // add some comments randomly
            if (random.nextBoolean()) {
                addRandomComment(saved, admin, user);
            }
        }
    }

    private void addRandomComment(Task task, User admin, User user) {
        Comment comment = new Comment();
        comment.setTask(task);
        comment.setUser(Math.random() < 0.5 ? admin : user);
        comment.setContent("Auto-generated comment for " + task.getName());
        comment.setCreatedAt(Instant.now());
        commentService.saveComment(comment);
    }
}
