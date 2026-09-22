package com.crazynoisyquiz.backend.room.service;

import com.crazynoisyquiz.backend.room.dto.CreateRoomRequest;
import com.crazynoisyquiz.backend.room.dto.RoomResponse;
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
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
// Centraliza a criação das salas e as regras do código de acesso.
public class RoomService {

    private static final int DEFAULT_MAX_PLAYERS = 8;
    private static final String ROOM_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final QuizRoomRepository quizRoomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserRepository userRepository;

    @Transactional
    // O dono da sala vem do usuário autenticado, nunca do corpo da requisição.
    public RoomResponse create(CreateRoomRequest request, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        QuizRoom room = QuizRoom.builder()
                .code(generateUniqueCode())
                .owner(owner)
                .status(RoomStatus.WAITING)
                .maxPlayers(resolveMaxPlayers(request))
                .build();

        QuizRoom savedRoom = quizRoomRepository.save(room);

        // Quem cria a sala já ocupa uma vaga e aparece na lista de participantes.
        RoomParticipant ownerParticipation = new RoomParticipant();
        ownerParticipation.setRoom(savedRoom);
        ownerParticipation.setUser(owner);
        ownerParticipation.setJoinedAt(Instant.now());
        roomParticipantRepository.save(ownerParticipation);

        return toResponse(savedRoom);

    }

    private Integer resolveMaxPlayers(CreateRoomRequest request) {
        if (request == null || request.getMaxPlayers() == null) {
            return DEFAULT_MAX_PLAYERS;
        }

        return request.getMaxPlayers();
    }

    private String generateUniqueCode() {
        // Se houver colisão, tentamos outro código antes de salvar a sala.
        String code;

        do {
            code = generateCode();
        } while (quizRoomRepository.existsByCode(code));
        return code;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(6);

        for (int i = 0; i < 6; i++) {
            int randomIndex = ThreadLocalRandom.current()
                    .nextInt(ROOM_CODE_CHARACTERS.length());

            code.append(ROOM_CODE_CHARACTERS.charAt(randomIndex));
        }
        return code.toString();
    }

    private RoomResponse toResponse(QuizRoom room) {
        // Retornamos apenas os dados necessários para o cliente conhecer a sala.
        return RoomResponse.builder()
                .id(room.getId())
                .code(room.getCode())
                .ownerId(room.getOwner().getId())
                .status(room.getStatus())
                .maxPlayers(room.getMaxPlayers())
                .createdAt(room.getCreatedAt())
                .build();
    }

}
