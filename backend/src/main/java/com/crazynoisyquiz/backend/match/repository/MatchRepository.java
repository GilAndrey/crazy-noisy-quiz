package com.crazynoisyquiz.backend.match.repository;

import com.crazynoisyquiz.backend.match.model.Match;
import com.crazynoisyquiz.backend.match.model.MatchStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    Page<Match> findAllByRoomId(UUID roomId, Pageable pageable);

    boolean existsByRoomIdAndStatus(UUID roomId, MatchStatus status);

    // Bloqueia primeiro as partidas para manter a mesma ordem usada no fim da rodada.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Match m where m.room.id in "
            + "(select r.id from QuizRoom r where r.code = :code) "
            + "and m.status = :status order by m.id")
    List<Match> findByRoomCodeAndStatusForUpdate(
            @Param("code") String code, @Param("status") MatchStatus status);

    // Evita que duas requisições abram rodadas ao mesmo tempo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Match m where m.id = :matchId")
    Optional<Match> findByIdForUpdate(@Param("matchId") UUID matchId);
}
