package com.fitplate.fitplateapi.user.repository;

import com.fitplate.fitplateapi.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 엔티티의 데이터 접근 계층
 *
 * 📌 Repository 역할:
 * - 사용자 정보를 데이터베이스에서 조회, 저장, 수정, 삭제
 * - SQL 쿼리를 직접 작성할 필요 없음
 * - JpaRepository<User, Long>: User 엔티티, Long 타입 ID
 *
 * 📌 기본 제공 메서드:
 * - save(User): 사용자 저장 (INSERT/UPDATE)
 * - findById(Long): ID로 사용자 조회
 * - findAll(): 모든 사용자 조회
 * - delete(User): 사용자 삭제
 *
 * 📌 커스텀 메서드 (우리가 정의):
 * - findByTossUserKey(): Toss 사용자 키로 사용자 조회
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Toss 사용자 키로 사용자 조회
     *
     * 📌 메서드 명명 규칙:
     * findBy + [필드명]
     * 자동으로 WHERE toss_user_key = ? SQL 생성
     *
     * 📌 사용 시나리오:
     * MealPlanService에서 주어진 tossUserKey로 사용자가 이미 존재하는지 확인
     *
     * 자동 생성 SQL:
     * SELECT * FROM users WHERE toss_user_key = ?
     *
     * 📌 사용 예:
     * Optional<User> user = userRepository.findByTossUserKey("MOCK_USER_001");
     *
     * if (user.isPresent()) {
     *     User existingUser = user.get();
     *     // 기존 사용자 사용
     * } else {
     *     // 새로운 사용자 생성
     * }
     *
     * @param tossUserKey Toss 사용자 고유 키
     * @return Optional: 사용자가 있으면 포함, 없으면 비어있음
     */
    Optional<User> findByTossUserKey(String tossUserKey);
}
