package com.jpr.clss.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpr.clss.entity.TeamInvite;

public interface TeamInviteRepository extends JpaRepository<TeamInvite, String> {
    List<TeamInvite> findByTeamIdOrderByCreatedAtDesc(String teamId);

    Optional<TeamInvite> findByToken(String token);

    long countByTeamIdAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(String teamId, Instant now);
}
