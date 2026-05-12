package com.jpr.clss.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "email_jobs")
public class EmailJob extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailType type;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String subject;

    @Column(length = 8000, nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailJobStatus status = EmailJobStatus.PENDING;

    @Column(nullable = false)
    private int attempts;

    @Column
    private Instant sentAt;

    @Column(length = 2000)
    private String lastError;
}
