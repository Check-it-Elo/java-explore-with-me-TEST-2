package ru.practicum.ewm.main.controller.public_;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.EventFullDto;
import ru.practicum.ewm.main.dto.EventShortDto;
import ru.practicum.ewm.main.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class PublicEventsController {
    private final EventService eventService;

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return req.getRemoteAddr();
    }

    @GetMapping
    public List<EventShortDto> search(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(defaultValue = "EVENT_DATE") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            HttpServletRequest request
    ) {
        if (rangeStart != null && !rangeStart.isBlank()
                && rangeEnd != null && !rangeEnd.isBlank()) {
            var fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            var start = java.time.LocalDateTime.parse(rangeStart, fmt);
            var end = java.time.LocalDateTime.parse(rangeEnd, fmt);
            if (end.isBefore(start)) {
                throw new ru.practicum.ewm.main.exception.BadRequestException("rangeEnd must be after or equal to rangeStart");
            }
        }
        String normalizedSort = "VIEWS".equalsIgnoreCase(sort) ? "VIEWS" : "EVENT_DATE";
        String ip  = clientIp(request);
        String uri = request.getRequestURI();
        return eventService.searchPublic(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable,
                normalizedSort, from, size, ip, uri
        );
    }

    @GetMapping("/{eventId}")
    public EventFullDto getById(@PathVariable long eventId, HttpServletRequest request) {
        String ip  = clientIp(request);
        String uri = request.getRequestURI();
        return eventService.getPublicEvent(eventId, ip, uri);
    }
}