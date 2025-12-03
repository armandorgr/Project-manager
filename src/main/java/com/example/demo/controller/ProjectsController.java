package com.example.demo.controller;

import com.example.demo.controller.anotations.projects.RequireProjectRole;
import com.example.demo.controller.dto.*;
import com.example.demo.controller.responses.Response;
import com.example.demo.mapper.ProjectMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.*;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.InvitationService;
import com.example.demo.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * REST controller responsible for managing projects and related actions such as
 * invitations, membership, and user access.
 * <p>
 * Provides endpoints for CRUD operations on projects and for handling invitations
 * to join projects.
 */
@Tag(name = "Projects", description = "Endpoints to manage projects and invitations")
@RestController
@RequestMapping("/api/project")
public class ProjectsController {

    private final ProjectMapper projectMapper;
    private final ProjectService projectService;
    private final CustomUserDetailsService usersService;
    private final InvitationService invitationService;
    private final UserMapper userMapper;

    public ProjectsController(
            ProjectMapper projectMapper,
            ProjectService projectService,
            CustomUserDetailsService usersService,
            InvitationService invitationService, UserMapper userMapper
    ) {
        this.projectMapper = projectMapper;
        this.projectService = projectService;
        this.usersService = usersService;
        this.invitationService = invitationService;
        this.userMapper = userMapper;
    }

    /**
     * Obtiene la lista de todos los miembros que pertenecen a un proyecto específico.
     * * Este endpoint requiere que el usuario esté autenticado y tenga al menos el rol 'USER'
     * en el proyecto especificado por {@code projectId}.
     *
     * @param projectId El identificador único (UUID) del proyecto del cual se desean obtener los miembros.
     * @return Una respuesta HTTP 200 (OK) que contiene una lista de DTOs de miembros del proyecto.
     * La respuesta sigue la estructura {@code Response<List<ProjectMemberDto>>}.
     */
    @Operation(summary = "Obtener lista de miembros del proyecto",
            description = "Recupera una lista de todos los usuarios asociados a un proyecto por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de miembros recuperada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Response.class))), // Asume Response es tu wrapper principal
            @ApiResponse(responseCode = "401", description = "No autorizado (Token JWT faltante o inválido)",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (El usuario no tiene el rol 'USER' o superior en el proyecto)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado",
                    content = @Content)
    })
    @RequireProjectRole(ProjectRole.USER)
    @GetMapping("/{id}/members")
    public ResponseEntity<Response<List<ProjectMemberDto>>> getMembers(
            @PathVariable("id") UUID projectId
    ) {
        this.projectService.getOneById(projectId);
        List<ProjectMemberDto> members = this.projectService.getMembers(projectId).stream().map(this.userMapper::toResponse).toList();
        Response<List<ProjectMemberDto>> response =
                new Response<>("SUCCESS", "Project members retrieved", members, null);
        return ResponseEntity.ok(response);
    }

    @RequireProjectRole(ProjectRole.ADMIN)
    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Response<String>> kickMember(
            @PathVariable("id") UUID projectId,
            @PathVariable("memberId") UUID memberId
    ){
        User member = (User) this.usersService.loadUserById(memberId);
        Project project = this.projectService.getOneById(projectId);
        this.projectService.kickUserFromProject(member, project);
        Response<String> response = new Response<>("SUCCESS", "Member kicked successfully", null, null);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }

    /**
     * Updates an existing project with new data.
     *
     * @param dto       DTO containing updated project fields
     * @param projectId UUID of the project to update
     * @return ResponseEntity with updated project data and HTTP 200 OK
     */
    @Operation(
            summary = "Update an existing project",
            description = "Updates an existing project with new data.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Project updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Project not found")
            }
    )
    @RequireProjectRole(ProjectRole.ADMIN)
    @PatchMapping("/{id}")
    public ResponseEntity<Response<ProjectResponseDto>> updateProject(
            @RequestBody @Valid UpdateProjectDto dto,
            @PathVariable("id") UUID projectId
    ) {
        Project project = this.projectService.getOneById(projectId);
        this.projectMapper.updateProjectFromDto(dto, project);
        Project updatedProject = this.projectService.saveProject(project);
        Response<ProjectResponseDto> response =
                new Response<>("SUCCESS", "Project updated", this.projectMapper.toResponse(updatedProject), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a project if the authenticated user has ADMIN privileges on it.
     *
     * @param currentUser current authenticated user
     * @param projectId   UUID of the project to delete
     * @return ResponseEntity with HTTP 204 NO_CONTENT if successful, or 403 if unauthorized
     */
    @Operation(
            summary = "Delete a project",
            description = "Deletes a project if the authenticated user has ADMIN privileges on it.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Project deleted successfully"),
                    @ApiResponse(responseCode = "403", description = "Unauthorized")
            }
    )
    @RequireProjectRole(ProjectRole.ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Response<ProjectResponseDto>> deleteProject(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("id") UUID projectId
    ) {
        UserHasProjects relation = this.projectService.getRelation(new UserProjectId(currentUser.getId(), projectId));
        Project project = relation.getProject();
        this.projectService.deleteProject(project);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Retrieves all projects the authenticated user is a member of.
     *
     * @param authentication current authenticated user
     * @return ResponseEntity with list of projects and HTTP 200 OK
     */
    @Operation(
            summary = "Get all projects for the user",
            description = "Retrieves all projects the authenticated user is a member of.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of projects retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    )
            }
    )
    @GetMapping
    public ResponseEntity<Response<List<ProjectResponseDto>>> getAll(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<ProjectResponseDto> projects = this.projectService.getAllProjectsByUser(currentUser.getId()).stream().map(this.projectMapper::toResponse).toList();
        return ResponseEntity.ok(new Response<>("SUCCESS", "All projects", projects, null));
    }

    /**
     * Retrieves a single project by its ID.
     *
     * @param projectId UUID of the project
     * @return ResponseEntity with project details and HTTP 200 OK
     */
    @Operation(
            summary = "Get a project by ID",
            description = "Retrieves a single project by its ID.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Project details retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Project not found")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<Response<ProjectResponseDto>> getOneById(@PathVariable("id") UUID projectId, @AuthenticationPrincipal User currentUser) {
        UserHasProjects relation = this.projectService.getRelation(new UserProjectId(currentUser.getId(), projectId));
        Response<ProjectResponseDto> response =
                new Response<>("SUCCESS", "Project data found", this.projectMapper.toResponse(relation), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Performs a query-based search of projects belonging to the current user.
     *
     * @param dto            ProjectQueryDto containing the search query
     * @param authentication current authenticated user
     * @return ResponseEntity with matching projects and HTTP 200 OK
     */
    @Operation(
            summary = "Search projects by query",
            description = "Performs a query-based search of projects belonging to the current user.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Query results retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    )
            }
    )
    @GetMapping("/query")
    public ResponseEntity<Response<List<ProjectResponseDto>>> getOneByQuery(
            @RequestBody @Valid ProjectQueryDto dto,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        List<ProjectResponseDto> project = this.projectService.getOneByQuery(dto.query(), currentUser.getId()).stream().map(this.projectMapper::toResponse).toList();
        Response<List<ProjectResponseDto>> response =
                new Response<>("SUCCESS", "Query results", project, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Sends an invitation to another user to join the given project.
     *
     * @param projectId   UUID of the project
     * @param dto         InvitationDto containing receiver info
     * @param currentUser authenticated user
     * @return ResponseEntity with HTTP 201 CREATED if invitation was sent successfully
     */
    @Operation(
            summary = "Send an invitation to join a project",
            description = "Sends an invitation to another user to join the given project.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Invitation sent successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request or user not found")
            }
    )
    @RequireProjectRole(ProjectRole.ADMIN)
    @PostMapping("/{id}/invite")
    public ResponseEntity<Response<String>> invite(
            @PathVariable("id") UUID projectId,
            @RequestBody @Valid InvitationDto dto,
            @AuthenticationPrincipal User currentUser
    ) {
        Project project = this.projectService.getOneById(projectId);
        String keyword = dto.email() == null ? dto.username() : dto.email();

        if (currentUser.getUsername().equals(keyword) || currentUser.getEmail().equals(keyword)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You can not invite yourself");
        }

        User receiver = (User) this.usersService.loadUserByEmailOrUsername(keyword);

        UserHasUser invitation = new UserHasUser(
                currentUser,
                receiver,
                project,
                String.format("%s has invited you to join %s", currentUser.getUsername(), project.getName())
        );
        try {
            UserHasUser existing = this.invitationService.findByReceiverAndProject(receiver, project);
            throw new DuplicateKeyException("User already invited");
        } catch (NoSuchElementException ex) {
            this.invitationService.sendInvitation(invitation);
        }
        Response<String> response =
                new Response<>("SUCCESS", "Invitation sent successfully", null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Handles a user's response (accept or decline) to a project invitation.
     *
     * @param projectId          UUID of the project
     * @param invitationResponse enum value representing the user's decision
     * @param authentication     current authenticated user
     * @return ResponseEntity with HTTP 200 OK if processed successfully
     */
    @Operation(
            summary = "Respond to a project invitation",
            description = "Handles a user's response (accept or decline) to a project invitation.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Invitation response processed successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Invitation or project not found")
            }
    )
    @PostMapping("/{id}/join")
    public ResponseEntity<Response<String>> join(
            @PathVariable("id") UUID projectId,
            @RequestParam InvitationResponseType invitationResponse,
            Authentication authentication
    ) {
        Project project = this.projectService.getOneById(projectId);
        User currentUser = (User) authentication.getPrincipal();
        UserHasUser invitation = this.invitationService.findByReceiverAndProject(currentUser, project);

        if (invitationResponse == InvitationResponseType.ACCEPT) {
            this.projectService.addUserToProject(new UserHasProjects(currentUser, project, ProjectRole.USER));
        }

        this.invitationService.deleteInvitation(invitation);
        Response<String> response = new Response<>("SUCCESS", "Invitation response processed", null, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new project and assigns the authenticated user as ADMIN.
     *
     * @param dto            DTO containing new project data
     * @param authentication current authenticated user
     * @return ResponseEntity with created project and HTTP 201 CREATED
     */
    @Operation(
            summary = "Create a new project",
            description = "Creates a new project and assigns the authenticated user as ADMIN.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Project created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    @Transactional
    @PostMapping
    public ResponseEntity<Response<ProjectResponseDto>> create(
            @RequestBody @Valid CreateProjectDto dto,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        Project project = projectMapper.toEntity(dto);
        project.setStartDate(Instant.now());
        Project saved = this.projectService.saveProject(project);

        this.projectService.addUserToProject(new UserHasProjects(currentUser, saved, ProjectRole.ADMIN));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Response<>("SUCCESS", "Project created", this.projectMapper.toResponse(saved), null));
    }
}
