package ru.practicum.ewm.main.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.main.model.Comment;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByEvent_IdOrderByCreatedOnDesc(Long eventId, Pageable pageable);

    Optional<Comment> findByIdAndAuthor_Id(Long id, Long authorId);

}