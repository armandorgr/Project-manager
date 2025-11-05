package com.example.demo.mapper;

import com.example.demo.controller.dto.InvitationResponseDto;
import com.example.demo.model.UserHasUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvitationMapper {
    @Mapping(target = "sender", source = "sender.id")
    @Mapping(target = "projectId", source = "project.id")
    InvitationResponseDto toResponse(UserHasUser userHasUser);
}
