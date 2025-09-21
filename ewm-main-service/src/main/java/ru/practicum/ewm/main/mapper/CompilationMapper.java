package ru.practicum.ewm.main.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.main.dto.CompilationDto;
import ru.practicum.ewm.main.dto.EventShortDto;
import ru.practicum.ewm.main.dto.NewCompilationDto;
import ru.practicum.ewm.main.dto.UpdateCompilationRequest;
import ru.practicum.ewm.main.mapper.config.CentralMapperConfig;
import ru.practicum.ewm.main.model.Compilation;
import ru.practicum.ewm.main.model.Event;

import java.util.Set;

@Mapper(config = CentralMapperConfig.class, uses = {EventMapper.class})
public interface CompilationMapper {

    /**
     * Преобразует сущность компиляции в DTO.
     * Список событий в кратком виде (EventShortDto) формируется сервисом
     * и передаётся сюда уже готовым.
     */
    @Mapping(target = "events", source = "eventsShort")
    CompilationDto toDto(Compilation compilation, java.util.List<EventShortDto> eventsShort);

    /**
     * Создаёт новую компиляцию на основе DTO и набора событий.
     * Поле id всегда игнорируется.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", source = "events")
    Compilation fromNew(NewCompilationDto dto, Set<Event> events);

    /**
     * Обновляет существующую компиляцию.
     * Поля с null-значениями не перезаписываются.
     * Если передан новый список events, то он полностью заменяет старый.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "events", source = "events")
    void update(UpdateCompilationRequest dto, @MappingTarget Compilation comp, Set<Event> events);
}