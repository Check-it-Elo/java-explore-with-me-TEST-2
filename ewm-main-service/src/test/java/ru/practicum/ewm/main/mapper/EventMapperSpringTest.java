package ru.practicum.ewm.main.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.practicum.ewm.main.dto.EventFullDto;
import ru.practicum.ewm.main.dto.LocationDto;
import ru.practicum.ewm.main.dto.NewEventDto;
import ru.practicum.ewm.main.mapper.util.DateTimeMapper;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.Location;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.model.enums.EventState;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EventMapperSpringTest.Config.class
})
class EventMapperSpringTest {

    @TestConfiguration
    @ComponentScan(basePackages = "ru.practicum.ewm.main.mapper")
    static class Config {
        @Bean
        public DateTimeMapper dateTimeMapper() {
            return new DateTimeMapper();
        }
    }

    @Autowired
    private EventMapper eventMapper;

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

    @Test
    void fromNew_mapsDtoAndRefs_toEvent() {
        long userId = 1L;
        long catId = 2L;
        LocalDateTime when = LocalDateTime.now().plusDays(1);

        NewEventDto dto = NewEventDto.builder()
                .title("My title")
                .annotation("A".repeat(20))
                .description("D".repeat(20))
                .eventDate(when.format(FMT))
                .category(catId)
                .location(LocationDto.builder().lat(10f).lon(20f).build())
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .build();

        Category cat = category(catId);
        User initiator = user(userId);
        Location loc = location(7L);

        Event mapped = eventMapper.fromNew(dto, cat, initiator, loc);

        assertNotNull(mapped);
        assertEquals("My title", mapped.getTitle());
        assertEquals("A".repeat(20), mapped.getAnnotation());
        assertEquals("D".repeat(20), mapped.getDescription());
        assertEquals(when.withNano(0), mapped.getEventDate().withNano(0));
        assertEquals(cat, mapped.getCategory());
        assertEquals(initiator, mapped.getInitiator());
        assertEquals(loc, mapped.getLocation());
        assertTrue(mapped.getPaid());
        assertEquals(100, mapped.getParticipantLimit());
        assertTrue(mapped.getRequestModeration());
    }

    @Test
    void toFullDto_mapsEventAndStats() {
        long userId = 3L;
        long catId = 4L;
        LocalDateTime when = LocalDateTime.now().plusDays(2);

        Event event = Event.builder()
                .id(100L)
                .title("T")
                .annotation("A".repeat(20))
                .description("D".repeat(20))
                .initiator(user(userId))
                .category(category(catId))
                .location(location(7L))
                .eventDate(when)
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .build();

        long confirmed = 5L;
        long views = 12L;

        EventFullDto dto = eventMapper.toFullDto(event, confirmed, views);

        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals("T", dto.getTitle());
        assertEquals(confirmed, dto.getConfirmedRequests());
        assertEquals(views, dto.getViews());
        assertNotNull(dto.getCategory());
        assertEquals(catId, dto.getCategory().getId());
        assertNotNull(dto.getInitiator());
        assertEquals(userId, dto.getInitiator().getId());
        assertNotNull(dto.getEventDate());
        assertEquals(when.withNano(0), LocalDateTime.parse(dto.getEventDate(), FMT).withNano(0));
    }
}