package ru.practicum.ewm.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.NewUserRequest;
import ru.practicum.ewm.main.dto.UserDto;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.UserMapper;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.UserRepository;
import ru.practicum.ewm.main.service.UserService;
import ru.practicum.ewm.main.util.PageUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    @Override
    public UserDto create(NewUserRequest dto) {
        User user = userMapper.fromNew(dto);
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    @Override
    public void delete(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        userRepository.deleteById(userId);
    }

    @Override
    public List<UserDto> getAll(List<Long> ids, int from, int size) {
        Page<User> page;
        if (ids != null && !ids.isEmpty()) {
            page = userRepository.findAllByIdIn(ids, PageUtils.by(from, size));
        } else {
            page = userRepository.findAll(PageUtils.by(from, size));
        }
        return page.stream().map(userMapper::toDto).collect(Collectors.toList());
    }
}