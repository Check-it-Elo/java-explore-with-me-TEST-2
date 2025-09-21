package ru.practicum.ewm.main.mapper.util;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateTimeMapper {
    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern(PATTERN);

    @Named("formatLdt")
    public String format(LocalDateTime ldt) {
        return ldt == null ? null : ldt.format(FMT);
    }

    @Named("parseLdt")
    public LocalDateTime parse(String text) {
        return text == null ? null : LocalDateTime.parse(text, FMT);
    }
}