package ru.practicum.ewm.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.main.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.exception.BadRequestException;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements ru.practicum.ewm.main.service.RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestMapper requestMapper;

    @Override
    public List<ParticipationRequestDto> getUserRequests(long userId) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        return requestRepository.findAllByRequester(requester).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ParticipationRequestDto addRequest(long userId, long eventId) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot request participation in own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("You can participate only in published events");
        }
        if (requestRepository.existsByRequesterAndEvent(requester, event)) {
            throw new ConflictException("Request already exists");
        }

        if (event.getParticipantLimit() != null && event.getParticipantLimit() > 0) {
            long confirmed = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);
            if (confirmed >= event.getParticipantLimit()) {
                throw new ConflictException("The participant limit has been reached");
            }
        }

        boolean noModeration = Boolean.FALSE.equals(event.getRequestModeration());
        boolean noLimit = event.getParticipantLimit() != null && event.getParticipantLimit() == 0;

        RequestStatus status = (noModeration || noLimit) ? RequestStatus.CONFIRMED : RequestStatus.PENDING;

        ParticipationRequest pr = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(status)
                .build();

        pr = requestRepository.save(pr);
        return requestMapper.toDto(pr);
    }

    @Transactional
    @Override
    public ParticipationRequestDto cancelRequest(long userId, long requestId) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        ParticipationRequest pr = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " not found"));
        if (!pr.getRequester().getId().equals(requester.getId())) {
            throw new NotFoundException("Request with id=" + requestId + " not found for user=" + userId);
        }
        pr.setStatus(RequestStatus.CANCELED);
        pr = requestRepository.save(pr);
        return requestMapper.toDto(pr);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(long userId, long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " not found for user=" + userId);
        }
        return requestRepository.findAllByEvent(event).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public EventRequestStatusUpdateResult updateEventRequests(long userId, long eventId, EventRequestStatusUpdateRequest body) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " not found for user=" + userId);
        }

        // если подтверждение не требуется (модерация выключена или лимит = 0) — это бизнес-конфликт
        boolean auto = Boolean.FALSE.equals(event.getRequestModeration()) || (event.getParticipantLimit() != null && event.getParticipantLimit() == 0);
        if (auto) {
            throw new ConflictException("Confirmation is not required for this event");
        }

        List<Long> ids = body.getRequestIds() == null ? List.of() : body.getRequestIds();
        if (ids.isEmpty()) {
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(List.of())
                    .rejectedRequests(List.of())
                    .build();
        }

        String next = body.getStatus();
        if (!"CONFIRMED".equalsIgnoreCase(next) && !"REJECTED".equalsIgnoreCase(next)) {
            throw new BadRequestException("Unknown target status: " + next);
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(ids);

        // можно менять только PENDING
        if (requests.stream().anyMatch(r -> r.getStatus() != RequestStatus.PENDING)) {
            throw new ConflictException("Only pending requests can be modified");
        }

        long confirmedNow = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();

        List<ParticipationRequestDto> confirmedDtos = new ArrayList<>();
        List<ParticipationRequestDto> rejectedDtos = new ArrayList<>();

        if ("CONFIRMED".equalsIgnoreCase(next)) {
            for (ParticipationRequest r : requests) {
                if (limit > 0 && confirmedNow >= limit) {
                    throw new ConflictException("Participant limit reached");
                }
                r.setStatus(RequestStatus.CONFIRMED);
                confirmedNow++;
                confirmedDtos.add(requestMapper.toDto(r));
            }
            // как только лимит закрыт — оставшиеся PENDING у события отклоняем
            if (limit > 0 && confirmedNow >= limit) {
                requestRepository.rejectAllPendingByEventId(eventId);
            }
        } else { // REJECTED
            for (ParticipationRequest r : requests) {
                r.setStatus(RequestStatus.REJECTED);
                rejectedDtos.add(requestMapper.toDto(r));
            }
        }

        // сохраним все изменённые
        requestRepository.saveAll(requests);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedDtos)
                .rejectedRequests(rejectedDtos)
                .build();
    }
}