package com.fitplate.fitplateapi.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 정보를 저장하는 Entity 클래스
 * MySQL의 users 테이블과 매핑됨
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    /**
     * 사용자 고유 ID (Primary Key, Auto Increment)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    /**
     * Toss에서 제공하는 사용자 고유 키
     * (NOT NULL, UNIQUE)
     */
    @Column(name = "toss_user_key", nullable = false, unique = true, length = 100)
    private String tossUserKey;

    /**
     * 사용자 닉네임
     * (NULL 허용)
     */
    @Column(name = "nickname", length = 50)
    private String nickname;

    /**
     * 생성 시간 (자동 생성, NOT NULL)
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 시간 (자동 업데이트, NOT NULL)
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User 생성자
     * 
     * @param tossUserKey Toss 사용자 키
     * @param nickname 닉네임
     */
    public User(String tossUserKey, String nickname){
        this.tossUserKey = tossUserKey;
        this.nickname = nickname;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}
