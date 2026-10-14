package com.crazynoisyquiz.backend.room.service;

import com.crazynoisyquiz.backend.room.dto.CreateRoomRequest;
import com.crazynoisyquiz.backend.match.model.Match;
import com.crazynoisyquiz.backend.match.model.MatchStatus;
import com.crazynoisyquiz.backend.match.repository.MatchRepository;
import com.crazynoisyquiz.backend.shared.exception.ForbiddenOperationException;
import com.crazynoisyquiz.backend.shared.exception.ResourceConflictException;
import com.crazynoisyquiz.backend.room.dto.RoomParticipantResponse;
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
import java.util.List;
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
    private final MatchRepository matchRepository;

    // Fecha a sala sem apagar partidas, respostas ou pontos já registrados.
    @Transactional
    public RoomResponse closeRoom(String roomCode, String email) {
        // Partida antes da sala: evita disputar os bloqueios em ordem inversa ao finishRound.
        List<Match> activeMatches = matchRepository.findByRoomCodeAndStatusForUpdate(
                roomCode, MatchStatus.IN_PROGRESS
        );
        QuizRoom room = quizRoomRepository.findByCodeForUpdate(roomCode)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada"));
        if (!room.getOwner().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenOperationException("Apenas o criador pode encerrar a sala");
        }
        if (room.getStatus() == RoomStatus.CLOSED) {
            throw new ResourceConflictException("A sala já está fechada");
        }

        // Se uma partida começou entre as duas consultas, repetimos o pedido com segurança.
        if (activeMatches.isEmpty()
                && matchRepository.existsByRoomIdAndStatus(room.getId(), MatchStatus.IN_PROGRESS)) {
            throw new ResourceConflictException("Uma partida acabou de iniciar; tente encerrar a sala novamente");
        }

        Instant closedAt = Instant.now();
        for (Match match : activeMatches) {
            match.setStatus(MatchStatus.CANCELLED);
            match.setFinishedAt(closedAt);
        }
        room.setStatus(RoomStatus.CLOSED);
        room.setClosedAt(closedAt);
        return toResponse(room);
    }

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
        // Buscamos os jogadores que ainda estão ativos e montamos os DTOs da resposta.
        List<RoomParticipantResponse> participants = roomParticipantRepository
                .findAllByRoomIdAndLeftAtIsNull(room.getId())
                .stream()
                .map(participant -> RoomParticipantResponse.builder()
                        .id(participant.getId())
                        .roomId(participant.getRoom().getId())
                        .userId(participant.getUser().getId())
                        .roomCode(participant.getRoom().getCode())
                        .joinedAt(participant.getJoinedAt())
                        .username(participant.getUser().getUsername())
                        .avatarKey(participant.getUser().getAvatarKey())
                        .build())
                .toList();

        return RoomResponse.builder()
                .id(room.getId())
                .code(room.getCode())
                .ownerId(room.getOwner().getId())
                .status(room.getStatus())
                .maxPlayers(room.getMaxPlayers())
                .createdAt(room.getCreatedAt())
                .closedAt(room.getClosedAt())
                .participants(participants)
                .build();
    }

    @Transactional(readOnly = true)
    public RoomResponse findByCode(String roomCode) {
        QuizRoom room = quizRoomRepository.findByCode(roomCode)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada"));


        return toResponse(room);
    }

}
