package ru.practicum.ewm.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.main.dto.CategoryDto;
import ru.practicum.ewm.main.dto.NewCategoryDto;
import ru.practicum.ewm.main.mapper.config.CentralMapperConfig;
import ru.practicum.ewm.main.model.Category;

@Mapper(config = CentralMapperConfig.class)
public interface CategoryMapper {

    CategoryDto toDto(Category category);

    @Mapping(target = "id", ignore = true)
    Category fromNew(NewCategoryDto dto);
}