package com.example.demo.mapper;

import com.example.demo.controller.dto.ProjectMemberDto;
import com.example.demo.controller.dto.UserResponseDto;
import com.example.demo.model.User;
import com.example.demo.model.UserHasProjects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponseDto toResponse(User user);
    @Mapping(source = "user", target = "user")
    @Mapping(source = "role", target = "role")
    ProjectMemberDto toResponse(UserHasProjects relation);
}
