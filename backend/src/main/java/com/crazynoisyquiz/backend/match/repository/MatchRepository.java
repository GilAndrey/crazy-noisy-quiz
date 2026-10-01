package com.crazynoisyquiz.backend.match.repository;

import com.crazynoisyquiz.backend.match.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {
}
