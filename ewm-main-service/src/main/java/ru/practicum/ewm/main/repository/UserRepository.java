package ru.practicum.ewm.main.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.main.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findAllByIdIn(Iterable<Long> ids, Pageable pageable);
}