package ru.practicum.ewm.main.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByInitiator(User initiator, Pageable pageable);

    long countByCategory_Id(Long categoryId);

    // ---- ADMIN ----
    @Query("""
            SELECT e FROM Event e
            WHERE ( :#{#users == null || #users.isEmpty()} = true OR e.initiator.id IN :users )
              AND ( :#{#states == null || #states.isEmpty()} = true OR e.state IN :states )
              AND ( :#{#categories == null || #categories.isEmpty()} = true OR e.category.id IN :categories )
              AND e.eventDate >= COALESCE(:rangeStart, e.eventDate)
              AND e.eventDate <= COALESCE(:rangeEnd,   e.eventDate)
            """)
    Page<Event> searchAdmin(@Param("users") List<Long> users,
                            @Param("states") List<EventState> states,
                            @Param("categories") List<Long> categories,
                            @Param("rangeStart") LocalDateTime rangeStart,
                            @Param("rangeEnd") LocalDateTime rangeEnd,
                            Pageable pageable);

    // ---- PUBLIC ----
    @Query("""
       SELECT e FROM Event e
        WHERE e.state = ru.practicum.ewm.main.model.enums.EventState.PUBLISHED
          AND (
                :search IS NULL OR
                LOWER(e.annotation)  LIKE :search OR
                LOWER(e.description) LIKE :search OR
                LOWER(e.title)       LIKE :search
              )
          AND ( :#{#categories == null || #categories.isEmpty()} = true OR e.category.id IN :categories )
          AND ( :paid IS NULL OR e.paid = :paid )
          AND e.eventDate >= COALESCE(:rangeStart, e.eventDate)
          AND e.eventDate <= COALESCE(:rangeEnd,   e.eventDate)
       """)
    Page<Event> searchPublic(@Param("search") String search,
                             @Param("categories") List<Long> categories,
                             @Param("paid") Boolean paid,
                             @Param("rangeStart") LocalDateTime rangeStart,
                             @Param("rangeEnd") LocalDateTime rangeEnd,
                             Pageable pageable);
}