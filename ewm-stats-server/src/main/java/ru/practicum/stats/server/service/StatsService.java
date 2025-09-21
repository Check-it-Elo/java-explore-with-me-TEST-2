package ru.practicum.stats.server.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.mapper.StatsMapper;
import ru.practicum.stats.server.repo.EndpointHitRepository;

@Service
@RequiredArgsConstructor
public class StatsService {
    public static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EndpointHitRepository repository;

    @Transactional
    public void saveHit(EndpointHitDto dto) {
        repository.save(StatsMapper.toEntity(dto));
    }

    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        LocalDateTime startDt = LocalDateTime.parse(start, FORMATTER);
        LocalDateTime endDt = LocalDateTime.parse(end, FORMATTER);
        if (endDt.isBefore(startDt)) {
            throw new IllegalArgumentException("end must be after or equal to start");
        }
        boolean hasUris = uris != null && !uris.isEmpty();
        if (unique) {
            return hasUris ? repository.statsUniqueByUris(startDt, endDt, uris)
                    : repository.statsUnique(startDt, endDt);
        } else {
            return hasUris ? repository.statsAllByUris(startDt, endDt, uris)
                    : repository.statsAll(startDt, endDt);
        }
    }
}