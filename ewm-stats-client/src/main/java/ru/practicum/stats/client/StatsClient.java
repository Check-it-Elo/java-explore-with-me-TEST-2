package ru.practicum.stats.client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class StatsClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public StatsClient(String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    //Отправка информации о хите
    public void hit(EndpointHitDto dto) {
        restTemplate.postForLocation(baseUrl + "/hit", dto);
    }

    //Получение статистики
    public List<ViewStatsDto> stats(String start, String end, List<String> uris, boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/stats")
                .queryParam("start", encode(start))
                .queryParam("end", encode(end))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", uri);
            }
        }

        ResponseEntity<ViewStatsDto[]> response =
                restTemplate.getForEntity(builder.toUriString(), ViewStatsDto[].class);

        return List.of(Objects.requireNonNull(response.getBody()));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
