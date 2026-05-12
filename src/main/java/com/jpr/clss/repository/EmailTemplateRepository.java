package com.jpr.clss.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpr.clss.entity.EmailTemplate;
import com.jpr.clss.entity.EmailType;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, String> {
    Optional<EmailTemplate> findByTeamIdAndType(String teamId, EmailType type);
    List<EmailTemplate> findByTeamId(String teamId);
    void deleteByTeamId(String teamId);
}
