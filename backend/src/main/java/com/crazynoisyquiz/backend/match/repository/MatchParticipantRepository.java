package com.crazynoisyquiz.backend.match.repository;

import com.crazynoisyquiz.backend.match.model.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, UUID> {
}
