package com.crazynoisyquiz.backend.room.service;

import com.crazynoisyquiz.backend.room.dto.RoomParticipantResponse;
import com.crazynoisyquiz.backend.room.model.QuizRoom;
import com.crazynoisyquiz.backend.room.model.RoomParticipant;
import com.crazynoisyquiz.backend.room.model.RoomStatus;
import com.crazynoisyquiz.backend.room.repository.QuizRoomRepository;
import com.crazynoisyquiz.backend.room.repository.RoomParticipantRepository;
import com.crazynoisyquiz.backend.shared.exception.ResourceConflictException;
import com.crazynoisyquiz.backend.user.model.User;
import com.crazynoisyquiz.backend.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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
            throw new ResourceConflictException("Não é possível entrar em uma sala que já foi iniciada");
        }

        // O e-mail vem do token JWT do usuário autenticado.
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        if (roomParticipantRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(
                room.getId(),
                user.getId()
        )) {
            // Evita criar duas participações para o mesmo usuário.
            throw new ResourceConflictException(
                    "Usuário já está participando dessa sala"
            );
        }

        long participantsCount = roomParticipantRepository
                .countByRoomIdAndLeftAtIsNull(room.getId());

        if (participantsCount >= room.getMaxPlayers()) {
            // Apenas jogadores ativos ocupam vagas na sala.
            throw new ResourceConflictException("A sala está cheia");
        }

        // Criamos a participação somente depois de todas as regras serem aprovadas.
        RoomParticipant participant = new RoomParticipant();
        participant.setRoom(room);
        participant.setUser(user);
        participant.setJoinedAt(Instant.now());

        RoomParticipant savedParticipant = roomParticipantRepository.save(participant);

        return toResponse(savedParticipant);
    }

    @Transactional
    public void leaveRoom(String roomCode, String userEmail) {
        QuizRoom room = quizRoomRepository.findByCode(roomCode)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        RoomParticipant participant = roomParticipantRepository
                .findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário não está participando desta sala"
                ));

        // Mantemos o registro e apenas marcamos quando o jogador saiu.
        participant.setLeftAt(Instant.now());
        roomParticipantRepository.save(participant);
    }

    // Evita expor diretamente a entidade JPA na resposta da API.
    private RoomParticipantResponse toResponse(RoomParticipant participant) {
        return RoomParticipantResponse.builder()
                .id(participant.getId())
                .roomId(participant.getRoom().getId())
                .userId(participant.getUser().getId())
                .roomCode(participant.getRoom().getCode())
                .joinedAt(participant.getJoinedAt())
                .username(participant.getUser().getUsername())
                .avatarKey(participant.getUser().getAvatarKey())
                .build();
    }


    @Transactional(readOnly = true)
    // vai retornar os jogadores ativos na sala! :)
    public List<RoomParticipantResponse> findActiveParticipants(
            String roomCode
    ) {
        QuizRoom room = quizRoomRepository.findByCode(roomCode)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada"));

        return roomParticipantRepository
                .findAllByRoomIdAndLeftAtIsNull(room.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }


}
