package ru.practicum.ewm.main.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.main.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.mapper.RequestMapper;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.ParticipationRequest;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.model.enums.EventState;
import ru.practicum.ewm.main.model.enums.RequestStatus;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.ParticipationRequestRepository;
import ru.practicum.ewm.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    EventRepository eventRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ParticipationRequestRepository requestRepository;
    @Mock
    RequestMapper requestMapper;

    @InjectMocks
    RequestServiceImpl service;

    private static User user(long id) {
        return User.builder().id(id).name("u" + id).email("u" + id + "@mail.com").build();
    }

    private static Event event(long id, long initiatorId, int limit, boolean moderation, EventState state) {
        return Event.builder()
                .id(id)
                .initiator(user(initiatorId))
                .participantLimit(limit)
                .requestModeration(moderation)
                .state(state)
                .build();
    }

    private static ParticipationRequest req(long id, long eventId, long userId, RequestStatus status) {
        return ParticipationRequest.builder()
                .id(id)
                .created(LocalDateTime.now())
                .event(event(eventId, 999, 0, true, EventState.PUBLISHED))
                .requester(user(userId))
                .status(status)
                .build();
    }

    // ========== addRequest: нельзя на своё событие ==========

    @Test
    void addRequest_ownEvent_forbidden_conflict() {
        long userId = 10L;
        long eventId = 100L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(
                event(eventId, userId, 0, true, EventState.PUBLISHED)
        ));

        assertThrows(ConflictException.class, () -> service.addRequest(userId, eventId));
        verify(requestRepository, never()).save(any());
    }

    // ========== addRequest: превышен лимит участников ==========

    @Test
    void addRequest_participantLimitExceeded_conflict() {
        long userId = 11L;
        long eventId = 101L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        Event ev = event(eventId, 500L, 1, false, EventState.PUBLISHED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(ev));
        when(requestRepository.countByEventAndStatus(ev, RequestStatus.CONFIRMED)).thenReturn(1L);
        assertThrows(ConflictException.class, () -> service.addRequest(userId, eventId));
        verify(requestRepository, never()).save(any());
    }

    // ========== cancelRequest: свою заявку можно отменить ==========

    @Test
    void cancelRequest_own_ok_changesStatusToCanceled() {
        long userId = 12L;
        long requestId = 777L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));

        ParticipationRequest existing = req(requestId, 200L, userId, RequestStatus.PENDING);
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ParticipationRequestDto dto = ParticipationRequestDto.builder()
                .id(requestId)
                .status(RequestStatus.CANCELED.name())
                .build();
        when(requestMapper.toDto(any(ParticipationRequest.class))).thenReturn(dto);

        ParticipationRequestDto result = service.cancelRequest(userId, requestId);

        assertEquals(requestId, result.getId());
        assertEquals(RequestStatus.CANCELED.name(), result.getStatus());

        ArgumentCaptor<ParticipationRequest> captor = ArgumentCaptor.forClass(ParticipationRequest.class);
        verify(requestRepository).save(captor.capture());
        assertEquals(RequestStatus.CANCELED, captor.getValue().getStatus());
    }

// ========== updateEventRequests: авто-модерация запрещает ручное подтверждение ==========

    @Test
    void updateEventRequests_autoModeration_conflict() {
        long userId = 21L;
        long eventId = 301L;

        Event ev = event(eventId, userId, 0, false, EventState.PUBLISHED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(ev));

        EventRequestStatusUpdateRequest body = EventRequestStatusUpdateRequest.builder()
                .requestIds(java.util.List.of(1L, 2L))
                .status("CONFIRMED")
                .build();

        assertThrows(ConflictException.class, () -> service.updateEventRequests(userId, eventId, body));
        verify(requestRepository, never()).saveAll(any());
    }

// ========== updateEventRequests: подтверждение до закрытия лимита, затем отклонение остальных ==========

    @Test
    void updateEventRequests_confirm_ok_andRejectRestWhenLimitReached() {
        long userId = 22L;
        long eventId = 302L;
        int limit = 2;

        Event ev = event(eventId, userId, limit, true, EventState.PUBLISHED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(ev));

        // сейчас подтверждено 1 место
        when(requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)).thenReturn(1L);

        // хотим подтвердить одну PENDING-заявку
        ParticipationRequest r1 = req(900L, eventId, 777L, RequestStatus.PENDING);
        when(requestRepository.findAllById(java.util.List.of(900L))).thenReturn(java.util.List.of(r1));

        // маппер: делаем простой ответ по id+status
        when(requestMapper.toDto(any(ParticipationRequest.class))).thenAnswer(inv -> {
            ParticipationRequest r = inv.getArgument(0);
            return ParticipationRequestDto.builder().id(r.getId()).status(r.getStatus().name()).build();
        });

        EventRequestStatusUpdateRequest body = EventRequestStatusUpdateRequest.builder()
                .requestIds(java.util.List.of(900L))
                .status("CONFIRMED")
                .build();

        EventRequestStatusUpdateResult res = service.updateEventRequests(userId, eventId, body);

        // одна подтвердилась, rejected пустой
        assertEquals(1, res.getConfirmedRequests().size());
        assertTrue(res.getRejectedRequests().isEmpty());
        assertEquals("CONFIRMED", res.getConfirmedRequests().get(0).getStatus());

        // после достижения лимита сервис должен отклонить остальные pending у события
        verify(requestRepository).rejectAllPendingByEventId(eventId);
        verify(requestRepository).saveAll(any());
    }

// ========== updateEventRequests: отклонение заявок ==========

    @Test
    void updateEventRequests_reject_ok() {
        long userId = 23L;
        long eventId = 303L;

        Event ev = event(eventId, userId, 5, true, EventState.PUBLISHED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(ev));

        ParticipationRequest r1 = req(910L, eventId, 701L, RequestStatus.PENDING);
        ParticipationRequest r2 = req(911L, eventId, 702L, RequestStatus.PENDING);
        when(requestRepository.findAllById(java.util.List.of(910L, 911L))).thenReturn(java.util.List.of(r1, r2));

        when(requestMapper.toDto(any(ParticipationRequest.class))).thenAnswer(inv -> {
            ParticipationRequest r = inv.getArgument(0);
            return ParticipationRequestDto.builder().id(r.getId()).status(r.getStatus().name()).build();
        });

        EventRequestStatusUpdateRequest body = EventRequestStatusUpdateRequest.builder()
                .requestIds(java.util.List.of(910L, 911L))
                .status("REJECTED")
                .build();

        EventRequestStatusUpdateResult res = service.updateEventRequests(userId, eventId, body);

        assertEquals(2, res.getRejectedRequests().size());
        assertTrue(res.getConfirmedRequests().isEmpty());
        assertEquals("REJECTED", res.getRejectedRequests().get(0).getStatus());

        verify(requestRepository).saveAll(any());
        verify(requestRepository, never()).rejectAllPendingByEventId(anyLong());
    }

// ========== updateEventRequests: можно менять только PENDING → иначе Conflict ==========

    @Test
    void updateEventRequests_nonPending_conflict() {
        long userId = 24L;
        long eventId = 304L;

        Event ev = event(eventId, userId, 10, true, EventState.PUBLISHED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(ev));

        ParticipationRequest r1 = req(920L, eventId, 701L, RequestStatus.CONFIRMED);
        when(requestRepository.findAllById(java.util.List.of(920L))).thenReturn(java.util.List.of(r1));

        EventRequestStatusUpdateRequest body = EventRequestStatusUpdateRequest.builder()
                .requestIds(java.util.List.of(920L))
                .status("REJECTED")
                .build();

        assertThrows(ConflictException.class, () -> service.updateEventRequests(userId, eventId, body));
        verify(requestRepository, never()).saveAll(any());
    }

// ========== updateEventRequests: пустой список id → пустой результат ==========

    @Test
    void updateEventRequests_emptyList_returnsEmptyResult() {
        long userId = 25L;
        long eventId = 305L;

        Event ev = event(eventId, userId, 10, true, EventState.PUBLISHED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(ev));

        EventRequestStatusUpdateRequest body = EventRequestStatusUpdateRequest.builder()
                .requestIds(java.util.List.of())
                .status("CONFIRMED")
                .build();

        EventRequestStatusUpdateResult res = service.updateEventRequests(userId, eventId, body);

        assertTrue(res.getConfirmedRequests().isEmpty());
        assertTrue(res.getRejectedRequests().isEmpty());
        verify(requestRepository, never()).saveAll(any());
    }

}