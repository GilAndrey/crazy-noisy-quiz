package com.crazynoisyquiz.backend.room.service;

import com.crazynoisyquiz.backend.room.dto.RoomParticipantResponse;
import com.crazynoisyquiz.backend.room.model.QuizRoom;
import com.crazynoisyquiz.backend.room.model.RoomParticipant;
import com.crazynoisyquiz.backend.room.model.RoomStatus;
import com.crazynoisyquiz.backend.room.repository.QuizRoomRepository;
import com.crazynoisyquiz.backend.room.repository.RoomParticipantRepository;
import com.crazynoisyquiz.backend.user.model.User;
import com.crazynoisyquiz.backend.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RoomParticipantService {

    // Classe feita para aplicar as regras para o usuario entrar na sala

    private final QuizRoomRepository quizRoomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserRepository userRepository;

    @Transactional
    public RoomParticipantResponse joinRoom(
            String roomCode,
            String userEmail
    ) {
        // A sala é localizada pelo código compartilhado entre os jogadores
        QuizRoom room = quizRoomRepository.findByCode(roomCode)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada"));

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalStateException("Não é possível entrar em uma sala que já foi iniciada");
        }

        // O e-mail vem do token JWT do usuário autenticado.
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        if (roomParticipantRepository.existsByRoomIdAndUserId(
                room.getId(),
                user.getId()
        )) {
            // Evita criar duas participações para o mesmo usuário.
            throw new IllegalStateException(
                    "Usuário já está participando dessa sala"
            );
        }

        long participantsCount = roomParticipantRepository
                .countByRoomIdAndLeftAtIsNull(room.getId());

        if (participantsCount >= room.getMaxPlayers()) {
            // Apenas jogadores ativos ocupam vagas na sala.
            throw new IllegalStateException("A sala está cheia");
        }

        // Criamos a participação somente depois de todas as regras serem aprovadas.
        RoomParticipant participant = new RoomParticipant();
        participant.setRoom(room);
        participant.setUser(user);
        participant.setJoinedAt(Instant.now());

        RoomParticipant savedParticipant = roomParticipantRepository.save(participant);

        return toResponse(savedParticipant);
    }

    // Evita expor diretamente a entidade JPA na resposta da API.
    private RoomParticipantResponse toResponse(RoomParticipant participant) {
        return RoomParticipantResponse.builder()
                .id(participant.getId())
                .roomId(participant.getRoom().getId())
                .userId(participant.getUser().getId())
                .roomCode(participant.getRoom().getCode())
                .joinedAt(participant.getJoinedAt())
                .build();
    }


}
