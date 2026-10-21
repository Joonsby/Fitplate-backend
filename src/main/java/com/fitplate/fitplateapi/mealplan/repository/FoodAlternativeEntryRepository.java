// 저장된 음식 대안 조회입니다.
package com.fitplate.fitplateapi.mealplan.repository;

import com.fitplate.fitplateapi.mealplan.domain.FoodAlternativeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodAlternativeEntryRepository extends JpaRepository<FoodAlternativeEntry, Long> {

    /** 칼로리 범위 안의 대안을 먼저 저장된 순으로. */
    List<FoodAlternativeEntry> findBySourceKeyAndCaloriesBetweenOrderByIdAsc(String sourceKey, int min, int max);

    boolean existsBySourceKeyAndCandidateKey(String sourceKey, String candidateKey);
}
