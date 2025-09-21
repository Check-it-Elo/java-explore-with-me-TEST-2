package ru.practicum.ewm.main.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageUtils {
    private PageUtils() {
    }

    public static Pageable by(int from, int size) {
        return by(from, size, Sort.unsorted());
    }

    public static Pageable by(int from, int size, Sort sort) {
        if (size <= 0) throw new IllegalArgumentException("size must be > 0");
        int page = from / size;
        return PageRequest.of(page, size, sort);
    }
}