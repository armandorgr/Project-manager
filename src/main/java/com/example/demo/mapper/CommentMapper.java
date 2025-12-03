package com.example.demo.mapper;

import com.example.demo.controller.dto.CommentResponseDto;
import com.example.demo.controller.dto.CreateCommentDto;
import com.example.demo.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface CommentMapper {
    CommentMapper INSTANCE = Mappers.getMapper(CommentMapper.class);

    //Methods
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "task", ignore = true)
    Comment toEntity(CreateCommentDto dto);

    @Mapping(target = "user", source = "user")
    CommentResponseDto toResponse(Comment comment);

    void updateFromDto(CreateCommentDto dto, @MappingTarget Comment comment);
}
