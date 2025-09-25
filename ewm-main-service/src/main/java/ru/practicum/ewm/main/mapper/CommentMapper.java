package ru.practicum.ewm.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.main.dto.comment.CommentDto;
import ru.practicum.ewm.main.mapper.util.DateTimeMapper;
import ru.practicum.ewm.main.model.Comment;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface CommentMapper {

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "createdOn", source = "createdOn")
    @Mapping(target = "updatedOn", source = "updatedOn")
    CommentDto toDto(Comment entity);
}