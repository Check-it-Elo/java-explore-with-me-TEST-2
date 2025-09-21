package ru.practicum.ewm.main.service;

import ru.practicum.ewm.main.dto.NewUserRequest;
import ru.practicum.ewm.main.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto create(NewUserRequest dto);

    void delete(long userId);

    List<UserDto> getAll(List<Long> ids, int from, int size);
}