package com.example.demo.mapper;

import com.example.demo.controller.dto.CreateProjectDto;
import com.example.demo.controller.dto.ProjectResponseDto;
import com.example.demo.controller.dto.UpdateProjectDto;
import com.example.demo.model.Project;
import com.example.demo.model.UserHasProjects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProjectMapper {
    ProjectMapper INSTANCE = Mappers.getMapper(ProjectMapper.class);
    Project toEntity(CreateProjectDto dto);
    void updateProjectFromDto(UpdateProjectDto dto, @MappingTarget Project project);
    ProjectResponseDto toResponse(Project project);

    @Mapping(target = "id", source = "project.id")
    @Mapping(target = "name", source = "project.name")
    @Mapping(target = "description", source = "project.description")
    @Mapping(target = "startDate", source = "project.startDate")
    @Mapping(target = "endDate", source = "project.endDate")
    @Mapping(target = "role", source = "role")
    ProjectResponseDto toResponse(UserHasProjects relation);
}
