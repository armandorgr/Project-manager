package com.example.demo.controller.anotations.projects;

import com.example.demo.model.ProjectRole;

import java.lang.annotation.*;

/**
 * Anotación para restringir el acceso a métodos basándose en el rol del usuario dentro de un proyecto.
 * Usa AOP para verificar el rol antes de ejecutar el método.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireProjectRole {
    ProjectRole value(); // Rol mínimo requerido
}
