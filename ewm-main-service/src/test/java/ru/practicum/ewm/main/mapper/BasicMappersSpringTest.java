package ru.practicum.ewm.main.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.practicum.ewm.main.dto.CategoryDto;
import ru.practicum.ewm.main.dto.UserShortDto;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.User;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = BasicMappersSpringTest.Config.class)
class BasicMappersSpringTest {

    @TestConfiguration
    @ComponentScan(basePackages = "ru.practicum.ewm.main.mapper")
    static class Config {
    }

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void category_toDto_mapsIdAndName() {
        Category src = Category.builder().id(42L).name("music").build();

        CategoryDto dto = categoryMapper.toDto(src);

        assertNotNull(dto);
        assertEquals(42L, dto.getId());
        assertEquals("music", dto.getName());
    }

    @Test
    void user_toShortDto_mapsIdAndName() {
        User u = User.builder().id(7L).name("Alice").email("alice@mail.com").build();

        UserShortDto dto = userMapper.toShortDto(u);

        assertNotNull(dto);
        assertEquals(7L, dto.getId());
        assertEquals("Alice", dto.getName());
    }
}