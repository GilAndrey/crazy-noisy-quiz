package com.crazynoisyquiz.backend.match.repository;

import com.crazynoisyquiz.backend.match.model.MatchRound;
import com.crazynoisyquiz.backend.match.model.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchRoundRepository extends JpaRepository<MatchRound, UUID> {

    boolean existsByMatchIdAndStatus(UUID matchId, RoundStatus status);

    Optional<MatchRound> findByMatchIdAndRoundNumber(
            UUID matchId,
            Integer roundNumber
    );
}
