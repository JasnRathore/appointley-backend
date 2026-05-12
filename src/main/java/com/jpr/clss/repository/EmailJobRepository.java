package com.jpr.clss.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpr.clss.entity.EmailJob;
import com.jpr.clss.entity.EmailJobStatus;

public interface EmailJobRepository extends JpaRepository<EmailJob, String> {
    List<EmailJob> findTop10ByStatusOrderByCreatedAtAsc(EmailJobStatus status);
}
