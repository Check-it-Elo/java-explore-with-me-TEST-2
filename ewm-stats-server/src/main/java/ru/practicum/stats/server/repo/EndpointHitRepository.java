package ru.practicum.stats.server.repo;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.model.EndpointHitEntity;

public interface EndpointHitRepository extends JpaRepository<EndpointHitEntity, Long> {

    @Query("""
           SELECT new ru.practicum.stats.dto.ViewStatsDto(h.app, h.uri, COUNT(h.id))
             FROM EndpointHitEntity h
            WHERE h.timestamp BETWEEN :start AND :end
            GROUP BY h.app, h.uri
            ORDER BY COUNT(h.id) DESC
           """)
    List<ViewStatsDto> statsAll(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    @Query("""
           SELECT new ru.practicum.stats.dto.ViewStatsDto(h.app, h.uri, COUNT(h.id))
             FROM EndpointHitEntity h
            WHERE h.timestamp BETWEEN :start AND :end
              AND h.uri IN :uris
            GROUP BY h.app, h.uri
            ORDER BY COUNT(h.id) DESC
           """)
    List<ViewStatsDto> statsAllByUris(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("uris") List<String> uris);

    @Query("""
           SELECT new ru.practicum.stats.dto.ViewStatsDto(h.app, h.uri, COUNT(DISTINCT h.ip))
             FROM EndpointHitEntity h
            WHERE h.timestamp BETWEEN :start AND :end
            GROUP BY h.app, h.uri
            ORDER BY COUNT(DISTINCT h.ip) DESC
           """)
    List<ViewStatsDto> statsUnique(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("""
           SELECT new ru.practicum.stats.dto.ViewStatsDto(h.app, h.uri, COUNT(DISTINCT h.ip))
             FROM EndpointHitEntity h
            WHERE h.timestamp BETWEEN :start AND :end
              AND h.uri IN :uris
            GROUP BY h.app, h.uri
            ORDER BY COUNT(DISTINCT h.ip) DESC
           """)
    List<ViewStatsDto> statsUniqueByUris(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end,
                                         @Param("uris") List<String> uris);
}