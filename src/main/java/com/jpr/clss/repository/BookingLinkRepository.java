package com.jpr.clss.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpr.clss.entity.BookingLink;

public interface BookingLinkRepository extends JpaRepository<BookingLink, String> {
    List<BookingLink> findByCreatorIdOrderByCreatedAtDesc(String creatorId);

    long countByCreatorIdAndActiveTrueAndExpirationDateAfter(String creatorId, Instant now);

    Optional<BookingLink> findByToken(String token);
    void deleteByCreatorId(String creatorId);
}
