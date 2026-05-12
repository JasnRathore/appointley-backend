package com.jpr.clss.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpr.clss.entity.BlockedDate;

public interface BlockedDateRepository extends JpaRepository<BlockedDate, String> {
    List<BlockedDate> findByUserIdOrderByStartsAtAsc(String userId);

    List<BlockedDate> findByUserIdAndEndsAtAfterAndStartsAtBefore(String userId, Instant from, Instant to);
    void deleteByUserId(String userId);
}
