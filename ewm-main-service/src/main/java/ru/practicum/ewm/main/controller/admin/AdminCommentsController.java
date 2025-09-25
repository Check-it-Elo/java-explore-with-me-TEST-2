package ru.practicum.ewm.main.controller.admin;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.service.CommentService;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class AdminCommentsController {

    private final CommentService commentService;

    @DeleteMapping("/{commentId}")
    public void deleteAny(@PathVariable @Min(1) long commentId) {
        commentService.deleteByAdmin(commentId);
    }
}