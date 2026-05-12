package com.jpr.clss.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpr.clss.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(String userId);
    void deleteByUserId(String userId);
}
