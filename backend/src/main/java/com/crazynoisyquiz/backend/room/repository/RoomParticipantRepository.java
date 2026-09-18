package com.crazynoisyquiz.backend.room.repository;

import com.crazynoisyquiz.backend.room.model.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Centraliza as consultas dos jogadores que participam das salas.
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, UUID> {

    // Evita que o mesmo usuário entre duas vezes na mesma sala.
    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

    // Retorna todos os jogadores vinculados à sala.
    List<RoomParticipant> findAllByRoomId(UUID roomId);

    // Conta apenas quem ainda não saiu da sala.
    long countByRoomIdAndLeftAtIsNull(UUID roomId);
}
