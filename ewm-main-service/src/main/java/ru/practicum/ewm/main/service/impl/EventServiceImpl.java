package ru.practicum.ewm.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.*;
import ru.practicum.ewm.main.dto.enums.StateAction;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.exception.BadRequestException;
import ru.practicum.ewm.main.mapper.EventMapper;
import ru.practicum.ewm.main.model.*;
import ru.practicum.ewm.main.model.enums.EventState;
import ru.practicum.ewm.main.model.enums.RequestStatus;
import ru.practicum.ewm.main.repository.*;
import ru.practicum.ewm.main.service.EventService;
import ru.practicum.ewm.main.stats.StatsClient;
import ru.practicum.ewm.main.util.PageUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ===== PRIVATE (USER) =====

    @Transactional
    @Override
    public EventFullDto createEvent(long userId, NewEventDto dto) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " not found"));
        Location location = locationRepository.save(
                Location.builder()
                        .lat(dto.getLocation().getLat().doubleValue())
                        .lon(dto.getLocation().getLon().doubleValue())
                        .build()
        );

        LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate(), FMT);
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Field: eventDate. Error: должно содержать дату, которая еще не наступила.");
        }

        Event event = eventMapper.fromNew(dto, category, initiator, location);
        if (event.getPaid() == null) event.setPaid(false);
        if (event.getParticipantLimit() == null) event.setParticipantLimit(0);
        if (event.getRequestModeration() == null) event.setRequestModeration(true);

        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        event = eventRepository.save(event);

        return eventMapper.toFullDto(event, 0L, 0L);
    }

    @Override
    public List<EventShortDto> getUserEvents(long userId, int from, int size) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        Page<Event> page = eventRepository.findAllByInitiator(initiator, PageUtils.by(from, size));
        return enrichShort(page.getContent());
    }

    @Override
    public EventFullDto getUserEvent(long userId, long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " not found for user=" + userId);
        }
        return enrichFull(event);
    }

    @Transactional
    @Override
    public EventFullDto updateUserEvent(long userId, long eventId, UpdateEventUserRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " not found for user=" + userId);
        }
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        // category/location меняем через сервис
        if (dto.getCategory() != null) {
            Category cat = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " not found"));
            event.setCategory(cat);
        }
        if (dto.getLocation() != null) {
            Location loc = locationRepository.save(Location.builder()
                    .lat(dto.getLocation().getLat().doubleValue())
                    .lon(dto.getLocation().getLon().doubleValue())
                    .build());
            event.setLocation(loc);
        }

        eventMapper.updateFromUser(dto, event);

        if (dto.getEventDate() != null) {
            LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate(), FMT);
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Event date must be at least 2 hours in the future");
            }
            event.setEventDate(eventDate);
        }

        if (dto.getStateAction() == StateAction.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        } else if (dto.getStateAction() == StateAction.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }

        event = eventRepository.save(event);
        return enrichFull(event);
    }

    // ===== ADMIN =====

    @Override
    public List<EventFullDto> searchAdmin(List<Long> users, List<String> states, List<Long> categories,
                                          String rangeStart, String rangeEnd, int from, int size) {
        List<EventState> st = null;
        if (states != null && !states.isEmpty()) {
            st = states.stream().map(s -> EventState.valueOf(s.toUpperCase())).collect(Collectors.toList());
        }

        LocalDateTime start = null;
        LocalDateTime end = null;
        try {
            start = (rangeStart == null || rangeStart.isBlank()) ? null : LocalDateTime.parse(rangeStart, FMT);
            end = (rangeEnd == null || rangeEnd.isBlank()) ? null : LocalDateTime.parse(rangeEnd, FMT);
        } catch (Exception e) {
            throw new BadRequestException("Incorrect date format. Expected pattern: yyyy-MM-dd HH:mm:ss");
        }

        Page<Event> page = eventRepository.searchAdmin(users, st, categories, start, end, PageUtils.by(from, size, Sort.by("id").descending()));
        return page.stream().map(this::enrichFull).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public EventFullDto updateAdminEvent(long eventId, UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

        // category/location обновляем явно
        if (dto.getCategory() != null) {
            Category cat = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " not found"));
            event.setCategory(cat);
        }
        if (dto.getLocation() != null) {
            Location loc = locationRepository.save(Location.builder()
                    .lat(dto.getLocation().getLat().doubleValue())
                    .lon(dto.getLocation().getLon().doubleValue())
                    .build());
            event.setLocation(loc);
        }

        eventMapper.updateFromAdmin(dto, event);

        if (dto.getEventDate() != null) {
            LocalDateTime ed = LocalDateTime.parse(dto.getEventDate(), FMT);
            if (ed.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new BadRequestException("Event date must be at least 1 hour in the future for publishing");
            }
            event.setEventDate(ed);
        }

        if ("PUBLISH_EVENT".equalsIgnoreCase(dto.getStateAction())) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
            }
            if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("Event date must be at least 1 hour after publish time");
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if ("REJECT_EVENT".equalsIgnoreCase(dto.getStateAction())) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Published event cannot be rejected");
            }
            event.setState(EventState.CANCELED);
        }

        event = eventRepository.save(event);
        return enrichFull(event);
    }

    // ===== PUBLIC =====

    @Override
    public List<EventShortDto> searchPublic(String text,
                                            List<Long> categories,
                                            Boolean paid,
                                            String rangeStart,
                                            String rangeEnd,
                                            Boolean onlyAvailable,
                                            String sort,
                                            int from,
                                            int size,
                                            String clientIp,
                                            String uri) {
        LocalDateTime start = null;
        LocalDateTime end = null;
        try {
            start = (rangeStart == null || rangeStart.isBlank()) ? null : LocalDateTime.parse(rangeStart, FMT);
            end = (rangeEnd == null || rangeEnd.isBlank()) ? null : LocalDateTime.parse(rangeEnd, FMT);
        } catch (Exception e) {
            throw new BadRequestException("Incorrect date format. Expected pattern: yyyy-MM-dd HH:mm:ss");
        }

        if (start != null && end != null && end.isBefore(start)) {
            throw new BadRequestException("rangeEnd must be after rangeStart");
        }

        // Если обе даты не заданы — по умолчанию показываем только будущие события
        if (start == null && end == null) {
            start = LocalDateTime.now();
        }

        // Подготовка параметра для поиска, чтобы не вызывать LOWER на параметре в JPQL
        String search = (text == null || text.isBlank()) ? null : "%" + text.toLowerCase() + "%";

        // фиксируем просмотр самого запроса (не валим логику при ошибке)
        try {
            statsClient.hit(uri, clientIp, LocalDateTime.now());
        } catch (Exception ex) {
            log.warn("Stats hit failed: {}", ex.toString());
        }

        // сортировка
        Sort s = "VIEWS".equalsIgnoreCase(sort) ? Sort.unsorted() : Sort.by("eventDate").ascending();

        Page<Event> page = eventRepository.searchPublic(
                search,
                (categories == null || categories.isEmpty()) ? null : categories,
                paid,
                start,
                end,
                PageUtils.by(from, size, s)
        );

        List<Event> events = page.getContent();

        // фильтр onlyAvailable
        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = events.stream().filter(this::hasAvailableSlots).collect(Collectors.toList());
        }

        // enrich + сортировка по VIEWS
        List<EventShortDto> result = enrichShort(events);
        if ("VIEWS".equalsIgnoreCase(sort)) {
            result.sort(Comparator.comparingLong(EventShortDto::getViews).reversed());
        }
        return result;
    }

    @Override
    public EventFullDto getPublicEvent(long eventId, String clientIp, String uri) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " not found");
        }

        // 1) считаем просмотры ДО отправки хитa
        long before = 0L;
        try {
            before = fetchViewsByEventIds(List.of(eventId)).getOrDefault(eventId, 0L);
        } catch (Exception ex) {
            log.warn("Stats views (before) failed: {}", ex.toString());
        }

        // 2) отправляем hit (не заваливаем основной поток)
        try {
            statsClient.hit(uri, clientIp, LocalDateTime.now());
        } catch (Exception ex) {
            log.warn("Stats hit failed: {}", ex.toString());
        }

        // 3) возвращаем ответ с гарантированным +1
        return enrichFull(event, before + 1);
    }

    // ===== Helpers =====

    private boolean hasAvailableSlots(Event e) {
        if (e.getParticipantLimit() == null || e.getParticipantLimit() == 0) return true;
        long confirmed = requestRepository.countByEventAndStatus(e, RequestStatus.CONFIRMED);
        return confirmed < e.getParticipantLimit();
    }

    private List<EventShortDto> enrichShort(List<Event> events) {
        Map<Long, Long> views = fetchViewsByEventIds(events.stream().map(Event::getId).collect(Collectors.toList()));
        return events.stream()
                .map(e -> eventMapper.toShortDto(e, countConfirmed(e), views.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    private EventFullDto enrichFull(Event e) {
        long v = fetchViewsByEventIds(List.of(e.getId())).getOrDefault(e.getId(), 0L);
        return eventMapper.toFullDto(e, countConfirmed(e), v);
    }

    private long countConfirmed(Event e) {
        return requestRepository.countByEventAndStatus(e, RequestStatus.CONFIRMED);
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private Map<Long, Long> fetchViewsByEventIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        List<String> uris = ids.stream().map(id -> "/events/" + id).collect(Collectors.toList());

        Map<String, Long> byUri;
        try {
            byUri = statsClient.views(
                    uris,
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    LocalDateTime.now().plusDays(1),
                    true
            );
        } catch (Exception ex) {
            log.warn("Stats views failed: {}", ex.toString());
            byUri = Collections.emptyMap();
        }

        Map<Long, Long> res = new HashMap<>();
        for (Long id : ids) {
            res.put(id, byUri.getOrDefault("/events/" + id, 0L));
        }
        return res;
    }

    private EventFullDto enrichFull(Event e, long views) {
        return eventMapper.toFullDto(e, countConfirmed(e), views);
    }

}