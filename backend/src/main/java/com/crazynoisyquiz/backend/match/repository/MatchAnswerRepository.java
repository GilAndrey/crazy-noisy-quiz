package com.crazynoisyquiz.backend.match.repository;

import com.crazynoisyquiz.backend.match.model.MatchAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchAnswerRepository extends JpaRepository<MatchAnswer, UUID> {
    // Confere se o participante já enviou uma resposta nessa rodada.
    boolean existsByRoundIdAndParticipantId(UUID roundId, UUID participantId);

    // Busca as respostas que vamos pontuar ao encerrar a rodada
    List<MatchAnswer> findAllByRoundId(UUID roundId);

    long countByRoundId(UUID roundId);
}
