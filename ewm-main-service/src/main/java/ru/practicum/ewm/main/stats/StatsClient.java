package ru.practicum.ewm.main.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.main.stats.dto.EndpointHitDto;
import ru.practicum.ewm.main.stats.dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class StatsClient {

    private final RestTemplate restTemplate;

    @Value("${stats-server.url:${stats.url:}}")
    private String statsBaseUrl;

    @Value("${app.name:ewm-main-service}")
    private String appName;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void hit(String uri, String ip, LocalDateTime ts) {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app(appName)
                .uri(uri)
                .ip(ip)
                .timestamp(ts.format(FMT))
                .build();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (statsBaseUrl == null || statsBaseUrl.isBlank()) return;
            restTemplate.exchange(statsBaseUrl + "/hit", HttpMethod.POST, new HttpEntity<>(dto, headers), Void.class);
        } catch (Exception ignored) {
            // статистика недоступна — бизнес-логику не валим
        }
    }

    public Map<String, Long> views(Collection<String> uris, LocalDateTime start, LocalDateTime end, boolean unique) {
        Map<String, Long> zeros = new HashMap<>();
        if (uris == null || uris.isEmpty()) return zeros;
        for (String u : uris) zeros.put(u, 0L);

        try {
            if (statsBaseUrl == null || statsBaseUrl.isBlank()) return zeros;
            UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(statsBaseUrl)
                    .path("/stats")
                    .queryParam("start", start.format(FMT))
                    .queryParam("end", end.format(FMT))
                    .queryParam("unique", unique);

            for (String u : uris) {
                b.queryParam("uris", u);
            }

            URI uri = b.encode().build(true).toUri();

            ResponseEntity<ViewStatsDto[]> resp = restTemplate.getForEntity(uri, ViewStatsDto[].class);
            ViewStatsDto[] body = resp.getBody();
            if (body != null) {
                for (ViewStatsDto v : body) {
                    zeros.put(v.getUri(), v.getHits() == null ? 0L : v.getHits());
                }
            }
            return zeros;
        } catch (Exception ignored) {
            // Любая ошибка статистики — как будто просмотров нет
            return zeros;
        }
    }

}
