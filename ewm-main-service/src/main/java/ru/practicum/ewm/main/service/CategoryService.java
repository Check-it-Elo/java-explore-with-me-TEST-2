package ru.practicum.ewm.main.service;

import ru.practicum.ewm.main.dto.CategoryDto;
import ru.practicum.ewm.main.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    // admin
    CategoryDto create(NewCategoryDto dto);

    void delete(long catId);

    CategoryDto update(long catId, CategoryDto dto);

    // public
    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(long catId);
}