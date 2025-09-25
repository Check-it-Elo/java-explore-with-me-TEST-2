package ru.practicum.ewm.main.controller.private_;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.comment.CommentDto;
import ru.practicum.ewm.main.dto.comment.NewCommentDto;
import ru.practicum.ewm.main.dto.comment.UpdateCommentDto;
import ru.practicum.ewm.main.service.CommentService;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateCommentsController {

    private final CommentService commentService;

    @PostMapping("/events/{eventId}/comments")
    public CommentDto add(@PathVariable long userId,
                          @PathVariable long eventId,
                          @RequestBody @Valid NewCommentDto body) {
        return commentService.addComment(userId, eventId, body);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentDto edit(@PathVariable long userId,
                           @PathVariable long commentId,
                           @RequestBody @Valid UpdateCommentDto body) {
        return commentService.editOwnComment(userId, commentId, body);
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteOwn(@PathVariable long userId,
                          @PathVariable @Min(1) long commentId) {
        commentService.deleteOwnComment(userId, commentId);
    }
}