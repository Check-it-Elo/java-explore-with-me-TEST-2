package ru.practicum.ewm.main.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.main.dto.*;
import ru.practicum.ewm.main.mapper.config.CentralMapperConfig;
import ru.practicum.ewm.main.mapper.util.DateTimeMapper;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.Location;
import ru.practicum.ewm.main.model.User;

@Mapper(
        config = CentralMapperConfig.class,
        uses = {CategoryMapper.class, UserMapper.class, LocationMapper.class, DateTimeMapper.class}
)
public interface EventMapper {

    // ===== entity -> DTO (short) =====
    @Mapping(target = "annotation", source = "event.annotation")
    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "eventDate", source = "event.eventDate", qualifiedByName = "formatLdt")
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "initiator", source = "event.initiator")
    @Mapping(target = "paid", source = "event.paid")
    @Mapping(target = "title", source = "event.title")
    @Mapping(target = "views", source = "views")
    EventShortDto toShortDto(Event event, Long confirmedRequests, Long views);

    // ===== entity -> DTO (full) =====
    @Mapping(target = "annotation", source = "event.annotation")
    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "createdOn", source = "event.createdOn", qualifiedByName = "formatLdt")
    @Mapping(target = "description", source = "event.description")
    @Mapping(target = "eventDate", source = "event.eventDate", qualifiedByName = "formatLdt")
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "initiator", source = "event.initiator")
    @Mapping(target = "location", source = "event.location")
    @Mapping(target = "paid", source = "event.paid")
    @Mapping(target = "participantLimit", source = "event.participantLimit")
    @Mapping(target = "publishedOn", source = "event.publishedOn", qualifiedByName = "formatLdt")
    @Mapping(target = "requestModeration", source = "event.requestModeration")
    @Mapping(target = "state", expression = "java(event.getState() == null ? null : event.getState().name())")
    @Mapping(target = "title", source = "event.title")
    @Mapping(target = "views", source = "views")
    EventFullDto toFullDto(Event event, Long confirmedRequests, Long views);

    // ===== new DTO -> entity =====
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "eventDate", source = "dto.eventDate", qualifiedByName = "parseLdt")
    @Mapping(target = "createdOn", ignore = true) // заполним в сервисе
    @Mapping(target = "publishedOn", ignore = true) // заполним при публикации
    @Mapping(target = "state", ignore = true)
    // заполним логикой (PENDING)
    Event fromNew(NewEventDto dto, Category category, User initiator, Location location);

    // ===== partial update (user) =====
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "annotation", source = "dto.annotation")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "title", source = "dto.title")
    @Mapping(target = "paid", source = "dto.paid")
    @Mapping(target = "participantLimit", source = "dto.participantLimit")
    @Mapping(target = "requestModeration", source = "dto.requestModeration")
    // eventDate
    @Mapping(target = "eventDate", source = "dto.eventDate", qualifiedByName = "parseLdt")
    // category/location задаём отдельными вызовами из сервиса (когда найдём их по id)
    void updateFromUser(UpdateEventUserRequest dto, @MappingTarget Event event);

    // ===== partial update (admin) =====
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "annotation", source = "dto.annotation")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "title", source = "dto.title")
    @Mapping(target = "paid", source = "dto.paid")
    @Mapping(target = "participantLimit", source = "dto.participantLimit")
    @Mapping(target = "requestModeration", source = "dto.requestModeration")
    @Mapping(target = "eventDate", source = "dto.eventDate", qualifiedByName = "parseLdt")
    void updateFromAdmin(UpdateEventAdminRequest dto, @MappingTarget Event event);
}