package com.crazynoisyquiz.backend.match.repository;

import com.crazynoisyquiz.backend.match.model.MatchRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchRoundRepository extends JpaRepository<MatchRound, UUID> {
}
