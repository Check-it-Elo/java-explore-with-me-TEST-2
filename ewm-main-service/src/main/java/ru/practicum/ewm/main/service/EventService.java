package ru.practicum.ewm.main.service;

import ru.practicum.ewm.main.dto.*;

import java.util.List;

public interface EventService {

    // private (пользователь)
    EventFullDto createEvent(long userId, NewEventDto dto);

    List<EventShortDto> getUserEvents(long userId, int from, int size);

    EventFullDto getUserEvent(long userId, long eventId);

    EventFullDto updateUserEvent(long userId, long eventId, UpdateEventUserRequest dto);

    // admin
    List<EventFullDto> searchAdmin(List<Long> users, List<String> states, List<Long> categories,
                                   String rangeStart, String rangeEnd, int from, int size);

    EventFullDto updateAdminEvent(long eventId, UpdateEventAdminRequest dto);

    // public
    List<EventShortDto> searchPublic(String text, List<Long> categories, Boolean paid,
                                     String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                     String sort, int from, int size, String clientIp, String uri);

    EventFullDto getPublicEvent(long eventId, String clientIp, String uri);

}