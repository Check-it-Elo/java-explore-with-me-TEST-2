package ru.practicum.ewm.main.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.EventFullDto;
import ru.practicum.ewm.main.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.main.service.EventService;
import org.springframework.web.bind.annotation.PatchMapping;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Validated
public class AdminEventsController {

    private final EventService eventService;

    @GetMapping
    public List<EventFullDto> search(@RequestParam(required = false) List<Long> users,
                                     @RequestParam(required = false) List<String> states,
                                     @RequestParam(required = false) List<Long> categories,
                                     @RequestParam(required = false) String rangeStart, // "yyyy-MM-dd HH:mm:ss"
                                     @RequestParam(required = false) String rangeEnd,   // "yyyy-MM-dd HH:mm:ss"
                                     @RequestParam(defaultValue = "0") @Min(0) int from,
                                     @RequestParam(defaultValue = "10") @Min(1) int size) {
        return eventService.searchAdmin(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(@PathVariable long eventId,
                               @RequestBody @Valid UpdateEventAdminRequest dto) {
        return eventService.updateAdminEvent(eventId, dto);
    }

}