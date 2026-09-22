package com.crazynoisyquiz.backend.room.repository;

import com.crazynoisyquiz.backend.room.model.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Centraliza as consultas dos jogadores que participam das salas.
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, UUID> {

    // Evita duplicidade entre os participantes que ainda estão na sala.
    boolean existsByRoomIdAndUserIdAndLeftAtIsNull(UUID roomId, UUID userId);

    // Conta apenas quem ainda não saiu da sala.
    long countByRoomIdAndLeftAtIsNull(UUID roomId);

    // Localiza a participação ativa do usuário para registrar sua saída.
    Optional<RoomParticipant> findByRoomIdAndUserIdAndLeftAtIsNull(
            UUID roomId,
            UUID userId
    );

    // Retorna apenas os participantes ativos da sala.
    List<RoomParticipant> findAllByRoomIdAndLeftAtIsNull(UUID roomId);

}
