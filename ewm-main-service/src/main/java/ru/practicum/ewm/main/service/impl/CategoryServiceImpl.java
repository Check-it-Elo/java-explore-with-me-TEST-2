package ru.practicum.ewm.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.CategoryDto;
import ru.practicum.ewm.main.dto.NewCategoryDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.CategoryMapper;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.repository.CategoryRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.service.CategoryService;
import ru.practicum.ewm.main.util.PageUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    @Override
    public CategoryDto create(NewCategoryDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category name must be unique");
        }
        Category cat = categoryMapper.fromNew(dto);
        cat = categoryRepository.save(cat);
        return categoryMapper.toDto(cat);
    }

    @Transactional
    @Override
    public void delete(long catId) {
        Category cat = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));

        long count = eventRepository.countByCategory_Id(cat.getId().longValue());
        if (count > 0) {
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.deleteById(catId);
    }

    @Transactional
    @Override
    public CategoryDto update(long catId, CategoryDto dto) {
        Category cat = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));

        if (dto.getName() != null) {
            // Если имя меняется на уже существующее — конфликт
            if (!dto.getName().equals(cat.getName()) && categoryRepository.existsByName(dto.getName())) {
                throw new ConflictException("Category name must be unique");
            }
            cat.setName(dto.getName());
        }
        cat = categoryRepository.save(cat);
        return categoryMapper.toDto(cat);
    }

    @Override
    public List<CategoryDto> getAll(int from, int size) {
        Page<Category> page = categoryRepository.findAll(PageUtils.by(from, size));
        return page.stream().map(categoryMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public CategoryDto getById(long catId) {
        Category cat = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        return categoryMapper.toDto(cat);
    }
}