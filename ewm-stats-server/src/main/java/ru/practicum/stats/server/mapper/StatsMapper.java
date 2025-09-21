package ru.practicum.stats.server.mapper;

import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.server.model.EndpointHitEntity;

public final class StatsMapper {
    private StatsMapper() {
    }

    public static EndpointHitEntity toEntity(EndpointHitDto dto) {
        EndpointHitEntity e = new EndpointHitEntity();
        e.setId(dto.getId());
        e.setApp(dto.getApp());
        e.setUri(dto.getUri());
        e.setIp(dto.getIp());
        e.setTimestamp(dto.getTimestamp());
        return e;
    }
}