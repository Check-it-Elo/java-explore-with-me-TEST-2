package ru.practicum.ewm.main.controller.public_;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.comment.CommentDto;
import ru.practicum.ewm.main.service.CommentService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events/{eventId}/comments")
public class PublicCommentsController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> list(@PathVariable long eventId,
                                 @RequestParam(defaultValue = "0") @Min(0) int from,
                                 @RequestParam(defaultValue = "10") @Min(1) int size) {
        return commentService.getEventComments(eventId, from, size);
    }
}