package com.jpr.clss.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpr.clss.entity.User;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
