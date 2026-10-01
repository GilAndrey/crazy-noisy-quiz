package com.crazynoisyquiz.backend.room.repository;

import com.crazynoisyquiz.backend.room.model.QuizRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface QuizRoomRepository extends JpaRepository<QuizRoom, UUID> {

    Optional<QuizRoom> findByCode(String code);

    // Evita que duas requisições iniciem partidas simultaneamente na mesma sala.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from QuizRoom room where room.code = :code")
    Optional<QuizRoom> findByCodeForUpdate(@Param("code") String code);

    boolean existsByCode(String code);
}
