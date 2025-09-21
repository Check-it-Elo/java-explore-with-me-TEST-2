package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.ParticipationRequest;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.model.enums.RequestStatus;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByRequester(User requester);

    List<ParticipationRequest> findAllByEvent(Event event);

    boolean existsByRequesterAndEvent(User requester, Event event);

    long countByEventAndStatus(Event event, RequestStatus status);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       update ParticipationRequest r
          set r.status = 'REJECTED'
        where r.event.id = :eventId
          and r.status = 'PENDING'
       """)
    int rejectAllPendingByEventId(@Param("eventId") Long eventId);

}