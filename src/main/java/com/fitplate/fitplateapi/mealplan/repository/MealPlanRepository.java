package com.fitplate.fitplateapi.mealplan.repository;

import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

/**
 * MealPlan 엔티티의 데이터 접근 계층. (PK 타입: Long)
 */
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    /**
     * 특정 사용자의 모든 식단을 최신 생성 순서로 조회.
     */
    @EntityGraph(attributePaths = "meals")
    List<MealPlan> findByUserOrderByCreatedAtDesc(User user);

    @Override
    @EntityGraph(attributePaths = "meals")
    Optional<MealPlan> findById(Long id);
}
