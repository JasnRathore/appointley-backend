package com.jpr.clss.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpr.clss.entity.TeamMember;

public interface TeamMemberRepository extends JpaRepository<TeamMember, String> {
    List<TeamMember> findByUserIdOrderByCreatedAtAsc(String userId);

    List<TeamMember> findByTeamIdOrderByCreatedAtAsc(String teamId);

    Optional<TeamMember> findByTeamIdAndUserId(String teamId, String userId);

    long countByTeamId(String teamId);
}
