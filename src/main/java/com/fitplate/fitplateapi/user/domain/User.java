package com.fitplate.fitplateapi.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 정보 Entity. users 테이블과 매핑.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    /** PK, AUTO_INCREMENT */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    /** Toss 사용자 고유 식별 키 */
    @Column(name = "toss_user_key", nullable = false, unique = true, length = 100)
    private String tossUserKey;

    /** 계정 생성 시간 (생성자에서 설정) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static User create(String tossUserKey) {
        User user = new User();
        user.tossUserKey = tossUserKey;
        return user;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
