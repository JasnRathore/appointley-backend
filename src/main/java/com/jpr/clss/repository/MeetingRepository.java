package com.jpr.clss.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpr.clss.entity.Meeting;
import com.jpr.clss.entity.MeetingStatus;

public interface MeetingRepository extends JpaRepository<Meeting, String> {
    List<Meeting> findByOrganizerIdOrderByStartsAtAsc(String organizerId);

    List<Meeting> findByOrganizerIdAndStartsAtAfterOrderByStartsAtAsc(String organizerId, Instant now);

    long countByOrganizerIdAndStartsAtAfterAndStatus(String organizerId, Instant now, MeetingStatus status);

    List<Meeting> findByOrganizerIdAndStatusAndEndsAtAfterAndStartsAtBefore(String organizerId, MeetingStatus status, Instant startsBefore, Instant endsAfter);
    void deleteByOrganizerId(String organizerId);
}
