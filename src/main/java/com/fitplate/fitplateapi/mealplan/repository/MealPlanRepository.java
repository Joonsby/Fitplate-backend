package com.fitplate.fitplateapi.mealplan.repository;

import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    Optional<MealPlan> findByUserAndPlanDate(User user, LocalDate planDate);

    /** 기간 안에 날짜가 지정된 식단 (기록·통계용). */
    @EntityGraph(attributePaths = "meals")
    List<MealPlan> findByUserAndPlanDateBetweenOrderByPlanDateAsc(User user, LocalDate from, LocalDate to);

    /** since 이후 만들었거나 먹은 끼니의 음식 이름별 등장 횟수. [이름, 횟수] 를 많은 순으로. */
    @Query("""
            select f.name, count(f) from MealPlanFood f
            join f.meal m join m.mealPlan p
            where p.user = :user and (p.createdAt >= :since or m.eatenAt >= :since)
            group by f.name
            order by count(f) desc, f.name asc
            """)
    List<Object[]> countRecentFoodNames(@Param("user") User user, @Param("since") LocalDateTime since);
}
