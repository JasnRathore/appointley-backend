package com.jpr.clss.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "email_templates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"team_id", "type"})
})
public class EmailTemplate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailType type;

    @Column(nullable = false, length = 500)
    private String subjectTemplate;

    @Column(nullable = false, length = 5000)
    private String bodyTemplate;

    @Column(nullable = false)
    private boolean active = true;
}
