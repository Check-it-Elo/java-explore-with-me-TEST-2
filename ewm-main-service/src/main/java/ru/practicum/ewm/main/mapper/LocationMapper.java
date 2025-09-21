package ru.practicum.ewm.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.main.dto.LocationDto;
import ru.practicum.ewm.main.mapper.config.CentralMapperConfig;
import ru.practicum.ewm.main.model.Location;

@Mapper(config = CentralMapperConfig.class)
public interface LocationMapper {

    @Mapping(target = "lat", expression = "java(loc.getLat() == null ? null : loc.getLat().floatValue())")
    @Mapping(target = "lon", expression = "java(loc.getLon() == null ? null : loc.getLon().floatValue())")
    LocationDto toDto(Location loc);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lat", expression = "java(dto.getLat() == null ? null : dto.getLat().doubleValue())")
    @Mapping(target = "lon", expression = "java(dto.getLon() == null ? null : dto.getLon().doubleValue())")
    Location fromDto(LocationDto dto);
}