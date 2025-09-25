package ru.practicum.ewm.main.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.dto.*;
import ru.practicum.ewm.main.dto.enums.StateAction;
import ru.practicum.ewm.main.exception.BadRequestException;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.EventMapper;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.Location;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.model.enums.EventState;
import ru.practicum.ewm.main.repository.*;
import ru.practicum.ewm.main.stats.StatsClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    EventRepository eventRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    LocationRepository locationRepository;
    @Mock
    ParticipationRequestRepository requestRepository;
    @Mock
    EventMapper eventMapper;
    @Mock
    StatsClient statsClient;

    @InjectMocks
    EventServiceImpl service;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static User user(long id) {
        return User.builder().id(id).name("u" + id).email("u" + id + "@mail.com").build();
    }

    private static Category category(long id) {
        return Category.builder().id(id).name("c" + id).build();
    }

    private static Location location(Long id) {
        return Location.builder().id(id).lat(55.0).lon(37.0).build();
    }

    private static Event eventWithState(EventState state, LocalDateTime when, long userId, long catId) {
        return Event.builder()
                .id(100L)
                .title("Title")
                .annotation("A".repeat(20))
                .description("D".repeat(20))
                .initiator(user(userId))
                .category(category(catId))
                .location(location(7L))
                .eventDate(when)
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .state(state)
                .build();
    }

    // ========== createEvent ==========

    @Test
    void createEvent_ok_savesPending_andReturnsDto() {
        long userId = 1L;
        long catId = 5L;
        LocalDateTime when = LocalDateTime.now().plusHours(3);

        NewEventDto dto = NewEventDto.builder()
                .annotation("A".repeat(20))
                .description("D".repeat(20))
                .eventDate(when.format(FMT))
                .category(catId)
                .location(LocationDto.builder().lat(10f).lon(20f).build())
                .title("My event")
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .build();

        // stubs
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(categoryRepository.findById(catId)).thenReturn(Optional.of(category(catId)));
        when(locationRepository.save(any(Location.class))).thenAnswer(inv -> {
            Location l = inv.getArgument(0);
            l.setId(7L);
            return l;
        });

        Event mapped = eventWithState(null, when, userId, catId);
        when(eventMapper.fromNew(eq(dto), any(Category.class), any(User.class), any(Location.class)))
                .thenReturn(mapped);

        // upon save
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        // result mapping
        EventFullDto resultDto = EventFullDto.builder().id(100L).title("My event").build();
        when(eventMapper.toFullDto(any(Event.class), eq(0L), eq(0L))).thenReturn(resultDto);

        EventFullDto result = service.createEvent(userId, dto);

        assertEquals(100L, result.getId());
        // проверим, что сохранили со статусом PENDING
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertEquals(EventState.PENDING, captor.getValue().getState());

        verify(eventMapper).toFullDto(any(Event.class), eq(0L), eq(0L));
        verifyNoMoreInteractions(statsClient); // в createEvent статистика не трогается
    }

    @Test
    void createEvent_tooSoon_throwsBadRequest() {
        long userId = 1L;
        long catId = 5L;
        LocalDateTime when = LocalDateTime.now().plusHours(1); // меньше 2 часов

        NewEventDto dto = NewEventDto.builder()
                .annotation("A".repeat(20))
                .description("D".repeat(20))
                .eventDate(when.format(FMT))
                .category(catId)
                .location(LocationDto.builder().lat(10f).lon(20f).build())
                .title("Soon")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(categoryRepository.findById(catId)).thenReturn(Optional.of(category(catId)));
        when(locationRepository.save(any(Location.class))).thenReturn(location(7L));

        assertThrows(BadRequestException.class, () -> service.createEvent(userId, dto));
        verify(eventRepository, never()).save(any());
    }

    // ========== updateUserEvent (пример негативного сценария) ==========

    @Test
    void updateUserEvent_published_cannotBeChanged() {
        long userId = 10L;
        long eventId = 100L;
        Event published = eventWithState(EventState.PUBLISHED, LocalDateTime.now().plusDays(1), userId, 5L);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(published));

        // пустой dto нас не интересует — важно состояние
        assertThrows(ConflictException.class, () ->
                service.updateUserEvent(userId, eventId, ru.practicum.ewm.main.dto.UpdateEventUserRequest.builder().build())
        );

        verify(eventRepository, never()).save(any());
    }

    // ========== updateAdminEvent ==========

    @Test
    void updateAdminEvent_publish_ok_changesStateToPublished() {
        long eventId = 200L;
        Event pending = eventWithState(EventState.PENDING, LocalDateTime.now().plusDays(1), 1L, 5L);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pending));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toFullDto(any(Event.class), anyLong(), anyLong()))
                .thenReturn(EventFullDto.builder().id(eventId).state("PUBLISHED").build());

        UpdateEventAdminRequest dto = UpdateEventAdminRequest.builder()
                .stateAction("PUBLISH_EVENT")
                .build();

        EventFullDto res = service.updateAdminEvent(eventId, dto);

        assertEquals(eventId, res.getId());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertEquals(EventState.PUBLISHED, captor.getValue().getState());
    }

    @Test
    void updateAdminEvent_reject_ok_changesStateToCanceled() {
        long eventId = 201L;
        Event pending = eventWithState(EventState.PENDING, LocalDateTime.now().plusDays(2), 2L, 6L);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pending));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toFullDto(any(Event.class), anyLong(), anyLong()))
                .thenReturn(EventFullDto.builder().id(eventId).state("CANCELED").build());

        UpdateEventAdminRequest dto = UpdateEventAdminRequest.builder()
                .stateAction("REJECT_EVENT")
                .build();

        EventFullDto res = service.updateAdminEvent(eventId, dto);

        assertEquals(eventId, res.getId());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertEquals(EventState.CANCELED, captor.getValue().getState());
    }

    @Test
    void updateAdminEvent_publish_fromNonPending_throwsConflict() {
        long eventId = 202L;
        Event published = eventWithState(EventState.PUBLISHED, LocalDateTime.now().plusDays(1), 1L, 5L);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(published));

        UpdateEventAdminRequest dto = UpdateEventAdminRequest.builder()
                .stateAction("PUBLISH_EVENT")
                .build();

        assertThrows(ConflictException.class, () -> service.updateAdminEvent(eventId, dto));
        verify(eventRepository, never()).save(any());
    }

    // ========== getPublicEvent / searchPublic ==========

    @Test
    void getPublicEvent_notPublished_throwsNotFound() {
        long eventId = 300L;
        Event pending = eventWithState(EventState.PENDING, LocalDateTime.now().plusDays(1), 1L, 5L);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pending));

        assertThrows(NotFoundException.class, () -> service.getPublicEvent(eventId, "127.0.0.1", "/events/" + eventId));

        verify(eventMapper, never()).toFullDto(any(), anyLong(), anyLong());
    }

    @Test
    void searchPublic_badDateRange_throwsBadRequest() {
        // from > to
        String rangeStart = LocalDateTime.now().plusDays(3).format(FMT);
        String rangeEnd = LocalDateTime.now().plusDays(1).format(FMT);

        assertThrows(BadRequestException.class, () ->
                service.searchPublic(
                        null, null, null,
                        rangeStart, rangeEnd, false,
                        null, 0, 10,
                        "127.0.0.1", "/events"
                )
        );
    }

    @Test
    void updateUserEvent_sendToReview_ok_changesStateToPending() {
        long userId = 11L;
        long eventId = 401L;

        // событие принадлежит этому пользователю и сейчас НЕ на модерации
        Event canceled = eventWithState(EventState.CANCELED, LocalDateTime.now().plusDays(2), userId, 3L);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(canceled));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toFullDto(any(Event.class), anyLong(), anyLong()))
                .thenReturn(EventFullDto.builder().id(eventId).state("PENDING").build());

        UpdateEventUserRequest dto = UpdateEventUserRequest.builder()
                .stateAction(StateAction.SEND_TO_REVIEW)
                .build();

        EventFullDto res = service.updateUserEvent(userId, eventId, dto);
        assertEquals(eventId, res.getId());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertEquals(EventState.PENDING, captor.getValue().getState());
    }

    @Test
    void updateUserEvent_cancelReview_ok_changesStateToCanceled() {
        long userId = 12L;
        long eventId = 402L;

        // событие принадлежит пользователю и сейчас в статусе PENDING (на модерации)
        Event pending = eventWithState(EventState.PENDING, LocalDateTime.now().plusDays(3), userId, 4L);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pending));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toFullDto(any(Event.class), anyLong(), anyLong()))
                .thenReturn(EventFullDto.builder().id(eventId).state("CANCELED").build());

        UpdateEventUserRequest dto = UpdateEventUserRequest.builder()
                .stateAction(StateAction.CANCEL_REVIEW)
                .build();

        EventFullDto res = service.updateUserEvent(userId, eventId, dto);
        assertEquals(eventId, res.getId());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertEquals(EventState.CANCELED, captor.getValue().getState());
    }

}