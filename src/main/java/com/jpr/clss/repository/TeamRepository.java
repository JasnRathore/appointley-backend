package com.jpr.clss.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpr.clss.entity.Team;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, String> {
    List<Team> findByOwnerId(String ownerId);
}
