package ru.practicum.ewm.main.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventAdminRequest {

    @Size(min = 20, max = 2000)
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000)
    private String description;

    /** Формат "yyyy-MM-dd HH:mm:ss" */
    private String eventDate;

    private LocationDto location;

    private Boolean paid;

    @Min(0)
    private Integer participantLimit;

    private Boolean requestModeration;

    /** "PUBLISH_EVENT" | "REJECT_EVENT" */
    private String stateAction;

    @Size(min = 3, max = 120)
    private String title;
}