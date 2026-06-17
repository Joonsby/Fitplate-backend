package com.fitplate.fitplateapi.user.repository;

import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUser(User user);

    Optional<UserProfile> findByUser_UserId(Long userId);

    boolean existsByUser(User user);
}