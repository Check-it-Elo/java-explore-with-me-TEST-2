package ru.practicum.ewm.main.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    CommentRepository commentRepository;
    @Mock
    EventRepository eventRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    CommentMapper commentMapper;

    @InjectMocks
    CommentServiceImpl service;

    private static User user(long id) {
        return User.builder().id(id).name("u" + id).email("u" + id + "@m.com").build();
    }

    private static Event event(long id, long initiatorId, EventState state) {
        return Event.builder().id(id).initiator(user(initiatorId)).state(state).build();
    }

    private static Comment comment(long id, long eventId, long authorId, String text, LocalDateTime created, LocalDateTime updated) {
        return Comment.builder()
                .id(id)
                .event(event(eventId, 999L, EventState.PUBLISHED))
                .author(user(authorId))
                .text(text)
                .createdOn(created)
                .updatedOn(updated)
                .build();
    }

    private static CommentDto dtoOf(Comment c) {
        return CommentDto.builder()
                .id(c.getId())
                .eventId(c.getEvent().getId())
                .authorId(c.getAuthor().getId())
                .text(c.getText())
                .createdOn("stub")
                .updatedOn(c.getUpdatedOn() == null ? null : "stub")
                .build();
    }

    // ===== addComment =====

    @Test
    void addComment_ok_publishedEvent_returnsDto_andSaves() {
        long userId = 1L, eventId = 10L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event(eventId, 100L, EventState.PUBLISHED)));

        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(777L);
            return c;
        });
        // простой маппинг заглушкой
        when(commentMapper.toDto(any(Comment.class))).thenAnswer(inv -> dtoOf(inv.getArgument(0)));

        CommentDto res = service.addComment(userId, eventId, NewCommentDto.builder().text("Hello").build());

        assertEquals(777L, res.getId());
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertEquals("Hello", captor.getValue().getText());
        assertNotNull(captor.getValue().getCreatedOn());
    }

    @Test
    void addComment_emptyText_badRequest() {
        long userId = 1L, eventId = 10L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event(eventId, 100L, EventState.PUBLISHED)));

        assertThrows(BadRequestException.class,
                () -> service.addComment(userId, eventId, NewCommentDto.builder().text("  ").build()));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_notPublished_conflict() {
        long userId = 1L, eventId = 10L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event(eventId, 100L, EventState.PENDING)));

        assertThrows(ConflictException.class,
                () -> service.addComment(userId, eventId, NewCommentDto.builder().text("Yo").build()));
        verify(commentRepository, never()).save(any());
    }

    // ===== getEventComments =====

    @Test
    void getEventComments_ok_returnsPagedList() {
        long eventId = 20L;
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event(eventId, 200L, EventState.PUBLISHED)));

        Comment c1 = comment(1L, eventId, 5L, "a", LocalDateTime.now(), null);
        Comment c2 = comment(2L, eventId, 5L, "b", LocalDateTime.now(), null);
        when(commentRepository.findByEvent_IdOrderByCreatedOnDesc(eq(eventId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(c1, c2)));

        when(commentMapper.toDto(any(Comment.class))).thenAnswer(inv -> dtoOf(inv.getArgument(0)));

        var list = service.getEventComments(eventId, 0, 10);
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).getId());
    }

    // ===== editOwnComment =====

    @Test
    void editOwnComment_withinWindow_ok_updatesText() {
        long userId = 3L, commentId = 33L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));

        Comment existing = comment(commentId, 300L, userId, "old", LocalDateTime.now().minusHours(4), null);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existing));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commentMapper.toDto(any(Comment.class))).thenAnswer(inv -> dtoOf(inv.getArgument(0)));

        CommentDto res = service.editOwnComment(userId, commentId, UpdateCommentDto.builder().text("new text").build());

        assertEquals("new text", res.getText());
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertEquals("new text", captor.getValue().getText());
        assertNotNull(captor.getValue().getUpdatedOn());
    }

    @Test
    void editOwnComment_outsideWindow_conflict() {
        long userId = 4L, commentId = 44L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));

        Comment existing = comment(commentId, 300L, userId, "old", LocalDateTime.now().minusHours(6), null);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class,
                () -> service.editOwnComment(userId, commentId, UpdateCommentDto.builder().text("x").build()));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void editOwnComment_notOwner_notFound() {
        long userId = 5L, commentId = 55L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));

        Comment existing = comment(commentId, 300L, 999L, "old", LocalDateTime.now(), null);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existing));

        assertThrows(NotFoundException.class,
                () -> service.editOwnComment(userId, commentId, UpdateCommentDto.builder().text("x").build()));
    }

    // ===== delete =====

    @Test
    void deleteOwnComment_ok() {
        long userId = 6L, commentId = 66L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        Comment existing = comment(commentId, 300L, userId, "old", LocalDateTime.now(), null);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existing));

        service.deleteOwnComment(userId, commentId);

        verify(commentRepository).deleteById(commentId);
    }

    @Test
    void deleteByAdmin_ok() {
        long commentId = 77L;
        when(commentRepository.existsById(commentId)).thenReturn(true);

        service.deleteByAdmin(commentId);

        verify(commentRepository).deleteById(commentId);
    }

    @Test
    void deleteByAdmin_notFound() {
        long commentId = 78L;
        when(commentRepository.existsById(commentId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.deleteByAdmin(commentId));
        verify(commentRepository, never()).deleteById(anyLong());
    }
}