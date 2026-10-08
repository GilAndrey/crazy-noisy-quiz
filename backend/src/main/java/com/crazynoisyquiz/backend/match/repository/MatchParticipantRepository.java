package com.crazynoisyquiz.backend.match.repository;

import com.crazynoisyquiz.backend.match.model.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, UUID> {

    boolean existsByMatchIdAndUserEmailIgnoreCase(UUID matchId, String email);

    Optional<MatchParticipant> findByMatchIdAndUserEmailIgnoreCase(UUID matchId, String email);
}
