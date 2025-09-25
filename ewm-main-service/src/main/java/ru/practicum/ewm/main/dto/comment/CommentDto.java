package ru.practicum.ewm.main.dto.comment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private Long eventId;
    private Long authorId;
    private String text;
    /**
     * "yyyy-MM-dd HH:mm:ss"
     */
    private String createdOn;
    /**
     * "yyyy-MM-dd HH:mm:ss" or null
     */
    private String updatedOn;
}