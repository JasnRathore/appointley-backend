package com.jpr.clss.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpr.clss.entity.AvailabilityRule;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, String> {
    List<AvailabilityRule> findByUserIdOrderByDayOfWeekAscStartTimeAsc(String userId);

    void deleteByUserId(String userId);
}
