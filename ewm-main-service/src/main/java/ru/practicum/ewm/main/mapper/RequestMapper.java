package ru.practicum.ewm.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.ewm.main.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.mapper.config.CentralMapperConfig;
import ru.practicum.ewm.main.model.ParticipationRequest;

import java.time.LocalDateTime;

@Mapper(config = CentralMapperConfig.class)
public interface RequestMapper {

    @Mapping(target = "event", source = "event.id")
    @Mapping(target = "requester", source = "requester.id")
    @Mapping(target = "created", source = "created", qualifiedByName = "formatIso")
    @Mapping(target = "status", source = "status")
    ParticipationRequestDto toDto(ParticipationRequest pr);

    @Named("formatIso")
    default String formatIso(LocalDateTime ldt) {
        return ldt == null ? null
                : ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

}
