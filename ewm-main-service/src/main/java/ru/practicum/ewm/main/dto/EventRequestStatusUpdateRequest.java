package ru.practicum.ewm.main.dto;

import lombok.*;
import jakarta.validation.constraints.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequestStatusUpdateRequest {
    private List<@NotNull Long> requestIds;
    /**
     * "CONFIRMED" | "REJECTED"
     */
    @NotBlank
    private String status;
}