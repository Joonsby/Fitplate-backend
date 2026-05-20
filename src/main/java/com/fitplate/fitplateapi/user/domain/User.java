package com.fitplate.fitplateapi.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 정보를 저장하는 Entity 클래스
 * MySQL의 users 테이블과 매핑됨
 *
 * 📌 Entity의 생명 주기:
 * 1. New: 새로운 객체 생성 (아직 DB와 무관)
 * 2. Managed: save()로 저장 (JPA가 추적)
 * 3. Detached: 트랜잭션 끝남 (JPA가 더 이상 추적 안 함)
 * 4. Removed: delete()로 삭제
 */
@Entity  // JPA가 관리하는 엔티티
@Table(name = "users")  // DB 테이블명: users
@Getter  // Lombok: 모든 필드의 getter 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // Lombok: 파라미터 없는 생성자 생성 (JPA 필수)
public class User {

    /**
     * 사용자 고유 ID (Primary Key, 자동으로 증가)
     *
     * @GeneratedValue(strategy = GenerationType.IDENTITY):
     * - AUTO_INCREMENT로 1, 2, 3, ... 자동 증가
     * - 각 사용자를 고유하게 식별
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    /**
     * Toss에서 제공하는 사용자 고유 키
     *
     * 📌 왜 이 필드를 사용하나?
     * - Toss 결제 시스템과 연동되는 사용자 고유 식별자
     * - 여러 기기에서 로그인해도 같은 사용자로 인식
     *
     * unique = true: 중복 불가 (유니크 제약조건)
     * nullable = false: 필수값
     * length = 100: 최대 100글자
     */
    @Column(name = "toss_user_key", nullable = false, unique = true, length = 100)
    private String tossUserKey;

    /**
     * 사용자 닉네임
     *
     * length = 50: 최대 50글자
     * nullable = true (기본값): 없어도 됨 (프로필 설정 전까지)
     */
    @Column(name = "nickname", length = 50)
    private String nickname;

    /**
     * 계정 생성 시간
     *
     * updatable = false: 한 번 생성되면 변경 불가
     * nullable = false: 필수값
     *
     * 자동으로 현재 시간이 저장되지는 않으므로
     * 생성자에서 명시적으로 설정
     *
     * 예: 2024-05-20 10:30:15
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 계정 정보 수정 시간
     *
     * nullable = false: 필수값
     * 자동으로 현재 시간이 저장되지는 않으므로
     * 생성자에서 명시적으로 설정
     *
     * 예: 2024-05-20 10:30:15 (처음) → 2024-05-21 15:45:20 (수정시)
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User 생성자
     * 
     * 📌 사용 시나리오:
     * MealPlanService.generateMealPlan()에서
     * 새로운 사용자를 생성할 필요가 있을 때 호출됨
     *
     * 예: new User("MOCK_USER_001", "test_user")
     *
     * @param tossUserKey Toss 사용자 키
     * @param nickname 닉네임
     */
    public User(String tossUserKey, String nickname) {
        this.tossUserKey = tossUserKey;
        this.nickname = nickname;

        // 현재 시간을 생성 시간과 수정 시간으로 설정
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}
