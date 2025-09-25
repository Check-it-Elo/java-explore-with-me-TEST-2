package ru.practicum.ewm.main.service;

import ru.practicum.ewm.main.dto.comment.CommentDto;
import ru.practicum.ewm.main.dto.comment.NewCommentDto;
import ru.practicum.ewm.main.dto.comment.UpdateCommentDto;

import java.util.List;

public interface CommentService {

    // public
    List<CommentDto> getEventComments(long eventId, int from, int size);

    // private (user)
    CommentDto addComment(long userId, long eventId, NewCommentDto dto);

    CommentDto editOwnComment(long userId, long commentId, UpdateCommentDto dto);

    void deleteOwnComment(long userId, long commentId);

    // admin
    void deleteByAdmin(long commentId);
}