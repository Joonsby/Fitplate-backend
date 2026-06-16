package com.fitplate.fitplateapi.user.repository;

import com.fitplate.fitplateapi.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 엔티티의 데이터 접근 계층.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Toss 사용자 키로 사용자 조회. 없으면 빈 Optional 반환.
     */
    Optional<User> findByTossUserKey(String tossUserKey);
}
