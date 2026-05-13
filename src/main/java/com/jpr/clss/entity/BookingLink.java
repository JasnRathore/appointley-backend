package com.jpr.clss.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "booking_links")
public class BookingLink extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Instant expirationDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private String timezone;

    @Column
    private String recipientEmail;

    @Column
    private Integer maxUsages;

    @Column(nullable = false)
    private int currentUsages = 0;
}
