package ru.practicum.ewm.main.service;

import ru.practicum.ewm.main.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.main.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    // пользовательские заявки
    List<ParticipationRequestDto> getUserRequests(long userId);

    ParticipationRequestDto addRequest(long userId, long eventId);

    ParticipationRequestDto cancelRequest(long userId, long requestId);

    // владелец события управляет заявками
    List<ParticipationRequestDto> getEventRequests(long userId, long eventId);

    EventRequestStatusUpdateResult updateEventRequests(long userId, long eventId, EventRequestStatusUpdateRequest body);
}