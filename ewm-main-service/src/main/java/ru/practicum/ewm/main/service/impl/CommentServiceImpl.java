package ru.practicum.ewm.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.comment.CommentDto;
import ru.practicum.ewm.main.dto.comment.NewCommentDto;
import ru.practicum.ewm.main.dto.comment.UpdateCommentDto;
import ru.practicum.ewm.main.exception.BadRequestException;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.CommentMapper;
import ru.practicum.ewm.main.model.Comment;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.model.enums.EventState;
import ru.practicum.ewm.main.repository.CommentRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements ru.practicum.ewm.main.service.CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    private static final int EDIT_WINDOW_HOURS = 5;

    @Override
    public List<CommentDto> getEventComments(long eventId, int from, int size) {
        // проверим существование события (и одновременно не выдадим комменты к несуществующим)
        eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));
        var page = commentRepository.findByEvent_IdOrderByCreatedOnDesc(eventId, PageRequest.of(from / size, size));
        return page.map(commentMapper::toDto).getContent();
    }

    @Transactional
    @Override
    public CommentDto addComment(long userId, long eventId, NewCommentDto dto) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Comments are allowed only for published events");
        }
        if (dto.getText() == null || dto.getText().trim().isEmpty()) {
            throw new BadRequestException("Comment text must not be empty");
        }
        Comment comment = Comment.builder()
                .event(event)
                .author(author)
                .text(dto.getText().trim())
                .createdOn(LocalDateTime.now())
                .build();
        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Transactional
    @Override
    public CommentDto editOwnComment(long userId, long commentId, UpdateCommentDto dto) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found"));

        if (!comment.getAuthor().getId().equals(author.getId())) {
            throw new NotFoundException("Comment with id=" + commentId + " not found for user=" + userId);
        }
        if (dto.getText() == null || dto.getText().trim().isEmpty()) {
            throw new BadRequestException("Comment text must not be empty");
        }
        // ограничение на редактирование — первые 5 часов
        if (comment.getCreatedOn() != null &&
                comment.getCreatedOn().isBefore(LocalDateTime.now().minusHours(EDIT_WINDOW_HOURS))) {
            throw new ConflictException("Editing is allowed only within first " + EDIT_WINDOW_HOURS + " hours");
        }
        comment.setText(dto.getText().trim());
        comment.setUpdatedOn(LocalDateTime.now());
        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Transactional
    @Override
    public void deleteOwnComment(long userId, long commentId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found"));

        if (!comment.getAuthor().getId().equals(author.getId())) {
            throw new NotFoundException("Comment with id=" + commentId + " not found for user=" + userId);
        }
        commentRepository.deleteById(commentId);
    }

    @Transactional
    @Override
    public void deleteByAdmin(long commentId) {
        // удаляем без проверки автора
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Comment with id=" + commentId + " not found");
        }
        commentRepository.deleteById(commentId);
    }
}