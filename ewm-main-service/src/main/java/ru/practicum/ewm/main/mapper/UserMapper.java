package ru.practicum.ewm.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.main.dto.NewUserRequest;
import ru.practicum.ewm.main.dto.UserDto;
import ru.practicum.ewm.main.dto.UserShortDto;
import ru.practicum.ewm.main.mapper.config.CentralMapperConfig;
import ru.practicum.ewm.main.model.User;

@Mapper(config = CentralMapperConfig.class)
public interface UserMapper {

    UserDto toDto(User user);

    UserShortDto toShortDto(User user);

    @Mapping(target = "id", ignore = true)
    User fromNew(NewUserRequest dto);
}