package com.crazynoisyquiz.backend.room.repository;

import com.crazynoisyquiz.backend.room.model.QuizRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuizRoomRepository extends JpaRepository<QuizRoom, UUID> {

    Optional<QuizRoom> findByCode(String code);

    boolean existsByCode(String code);
}
