// 음식 교체 이력 조회입니다.
package com.fitplate.fitplateapi.personalization.repository;

import com.fitplate.fitplateapi.personalization.domain.FoodReplacement;
import com.fitplate.fitplateapi.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FoodReplacementRepository extends JpaRepository<FoodReplacement, Long> {

    List<FoodReplacement> findTop50ByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDescIdDesc(
            User user, LocalDateTime since);
}
