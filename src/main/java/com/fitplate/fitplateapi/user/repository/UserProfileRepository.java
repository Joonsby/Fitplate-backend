package com.fitplate.fitplateapi.user.repository;

import com.fitplate.fitplateapi.user.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByTossUserKey(String tossUserKey);

    boolean existsByTossUserKey(String tossUserKey);
}