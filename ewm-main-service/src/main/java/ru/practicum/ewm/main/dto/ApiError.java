package ru.practicum.ewm.main.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiError {
    /**
     * Список стектрейсов или описаний ошибок
     */
    private List<String> errors;
    /**
     * Сообщение об ошибке
     */
    private String message;
    /**
     * Общее описание причины ошибки
     */
    private String reason;
    /**
     * Код статуса HTTP-ответа (строкой, как в спецификации)
     */
    private String status;
    /**
     * "yyyy-MM-dd HH:mm:ss"
     */
    private String timestamp;
}