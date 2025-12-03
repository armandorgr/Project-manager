package com.example.demo.mapper;

import com.example.demo.controller.dto.CreateTaskDto;
import com.example.demo.controller.dto.TaskResponseDto;
import com.example.demo.controller.dto.UpdateTaskDto;
import com.example.demo.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, uses = {UserMapper.class})
public interface TaskMapper {
    TaskMapper INSTANCE = Mappers.getMapper(TaskMapper.class);
    Task toEntity(CreateTaskDto dto);

    @Mapping(target = "user", ignore = true)
    void updateTaskFromDto(UpdateTaskDto dto, @MappingTarget Task task);

    @Mapping(source = "user", target = "assignedUser")
    @Mapping(source = "project.id", target = "project")
    TaskResponseDto toResponse(Task task);
}
