package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.main.model.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
}