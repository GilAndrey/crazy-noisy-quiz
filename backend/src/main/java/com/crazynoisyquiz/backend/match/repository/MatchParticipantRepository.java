package com.crazynoisyquiz.backend.match.repository;

import com.crazynoisyquiz.backend.match.model.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;
import java.util.List;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, UUID> {

    boolean existsByMatchIdAndUserEmailIgnoreCase(UUID matchId, String email);

    Optional<MatchParticipant> findByMatchIdAndUserEmailIgnoreCase(UUID matchId, String email);

    // Conta os jogadores que começaram essa partida.
    long countByMatchId(UUID matchId);

    // Busca os jogadores na ordem do resultado final.
    List<MatchParticipant> findAllByMatchIdOrderByTotalPointsDescCorrectAnswersDesc(UUID matchId);
}
