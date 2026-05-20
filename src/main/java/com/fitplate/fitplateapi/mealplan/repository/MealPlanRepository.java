package com.fitplate.fitplateapi.mealplan.repository;

import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MealPlan 엔티티의 데이터 접근 계층(Data Access Layer)
 *
 * 📌 Repository 패턴의 역할:
 * - 데이터베이스에 대한 모든 접근을 담당
 * - SQL 쿼리를 직접 작성하지 않아도 됨
 * - JPA가 자동으로 데이터베이스 작업 처리
 *
 * 📌 JpaRepository<MealPlan, Long> 의미:
 * - MealPlan: 관리할 엔티티 타입
 * - Long: Primary Key(ID)의 타입
 *
 * 📌 자동으로 제공되는 메서드:
 * - save(): INSERT/UPDATE
 * - findById(): SELECT (ID로 조회)
 * - findAll(): SELECT * (전체 조회)
 * - delete(): DELETE
 *
 * 📌 우리가 추가로 정의한 메서드:
 * - findByUserOrderByCreatedAtDesc(): 특정 사용자의 모든 식단 (최신순)
 * - findFirstByUserAndExpiresAtAfterOrderByCreatedAtDesc(): 특정 사용자의 유효한 식단 중 가장 최신
 */
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    /**
     * 특정 사용자의 모든 식단을 최신 생성 순서로 조회
     *
     * 📌 쿼리 메서드 명명 규칙:
     * findBy + [필드명] + Order + By + [정렬필드명] + [정렬방향]
     *
     * 자동으로 생성되는 SQL:
     * SELECT * FROM meal_plans
     * WHERE user_id = ?
     * ORDER BY created_at DESC
     *
     * 📌 사용 예:
     * User user = ... ;
     * List<MealPlan> plans = mealPlanRepository.findByUserOrderByCreatedAtDesc(user);
     *
     * @param user 사용자
     * @return 사용자의 모든 식단 (최신순)
     */
    List<MealPlan> findByUserOrderByCreatedAtDesc(User user);

    /**
     * 특정 사용자의 "유효한(아직 진행 중인)" 식단 중 가장 최신 하나를 조회
     *
     * 📌 쿼리 메서드 설명:
     * - findFirst: 첫 번째 결과만 반환
     * - ByUser: WHERE user_id = ?
     * - AndExpiresAtAfter: AND expires_at > ?
     * - OrderByCreatedAtDesc: ORDER BY created_at DESC
     *
     * 자동으로 생성되는 SQL:
     * SELECT * FROM meal_plans
     * WHERE user_id = ? AND expires_at > ?
     * ORDER BY created_at DESC
     * LIMIT 1
     *
     * 📌 사용 예:
     * User user = ... ;
     * Optional<MealPlan> activePlan =
     *     mealPlanRepository.findFirstByUserAndExpiresAtAfterOrderByCreatedAtDesc(
     *         user,
     *         LocalDateTime.now()
     *     );
     *
     * if (activePlan.isPresent()) {
     *     MealPlan plan = activePlan.get();
     *     // 사용자의 유효한 식단이 있음
     * }
     *
     * @param user 사용자
     * @param now 현재 시간 (이 시간 이후에 만료되는 식단 찾음)
     * @return Optional로 감싸진 식단 (있으면 반환, 없으면 빈 Optional)
     */
    Optional<MealPlan> findFirstByUserAndExpiresAtAfterOrderByCreatedAtDesc(
            User user,
            LocalDateTime now
    );
}