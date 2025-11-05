package com.example.demo.security.aspect;

import com.example.demo.controller.anotations.projects.RequireProjectRole;
import com.example.demo.model.ProjectRole;
import com.example.demo.model.User;
import com.example.demo.model.UserHasProjects;
import com.example.demo.service.ProjectService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Parameter;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Aspecto que valida que el usuario tenga el rol requerido
 * dentro del proyecto antes de ejecutar métodos anotados con {@link RequireProjectRole}.
 */
@Aspect
@Component
public class RequireProjectRoleAspect {
    private final Logger logger = LoggerFactory.getLogger(RequireProjectRoleAspect.class);
    private final ProjectService projectService;

    public RequireProjectRoleAspect(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Before("@annotation(com.example.demo.controller.anotations.projects.RequireProjectRole)")
    public void checkProjectRole(JoinPoint joinPoint) {
        this.logger.debug("Aspect activado");
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireProjectRole annotation = signature.getMethod().getAnnotation(RequireProjectRole.class);

        // Obtener el rol requerido
        ProjectRole requiredRole = annotation.value();

        // Obtener usuario actual
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User currentUser)) {
            throw new ResponseStatusException(FORBIDDEN, "No autenticado o usuario inválido");
        }

        // Buscar el parámetro projectId del método
        UUID projectId = extractProjectId(joinPoint);
        if (projectId == null) {
            throw new IllegalStateException("El método anotado debe tener un parámetro 'projectId' de tipo UUID");
        }

        // Obtener la relación usuario-proyecto
        UserHasProjects relation = projectService.getRelationSafe(currentUser.getId(), projectId)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "No perteneces a este proyecto"));

        // Verificar el rol
        if (!hasRequiredRole(relation.getRole(), requiredRole)) {
            throw new ResponseStatusException(FORBIDDEN, "Rol insuficiente para esta operación");
        }
    }

    private UUID extractProjectId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = ((MethodSignature) joinPoint.getSignature()).getMethod().getParameters();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals("projectId") && args[i] instanceof UUID uuid) {
                return uuid;
            }
        }
        return null;
    }

    private boolean hasRequiredRole(ProjectRole userRole, ProjectRole requiredRole) {
        return userRole.ordinal() >= requiredRole.ordinal();
    }
}
