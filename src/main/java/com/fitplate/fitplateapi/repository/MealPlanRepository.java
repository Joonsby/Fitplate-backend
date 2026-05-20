package com.fitplate.fitplateapi.repository;

import com.fitplate.fitplateapi.domain.MealPlan;
import com.fitplate.fitplateapi.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    List<MealPlan> findByUserOrderByCreatedAtDesc(User user);

    Optional<MealPlan> findFirstByUserAndExpiresAtAfterOrderByCreatedAtDesc(
            User user,
            LocalDateTime now
    );
}