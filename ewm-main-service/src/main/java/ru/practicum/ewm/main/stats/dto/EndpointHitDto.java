package ru.practicum.ewm.main.stats.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointHitDto {
    private String app;        // имя сервиса: ewm-main-service
    private String uri;        // например: /events/123
    private String ip;         // клиентский IP
    private String timestamp;  // "yyyy-MM-dd HH:mm:ss"
}