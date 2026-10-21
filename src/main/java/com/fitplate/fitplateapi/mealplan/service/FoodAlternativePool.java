// AI 가 만든 음식 대안을 저장해 두고 다음 요청에 다시 꺼내 씁니다. 사용자 취향 필터는 꺼내는 쪽에서 합니다.
package com.fitplate.fitplateapi.mealplan.service;

import com.fitplate.fitplateapi.mealplan.domain.FoodAlternativeEntry;
import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.repository.FoodAlternativeEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class FoodAlternativePool {

    private final FoodAlternativeEntryRepository repository;
    private final Clock clock;
    private final TransactionTemplate readOnlyTransaction;
    private final TransactionTemplate newTransaction;

    public FoodAlternativePool(FoodAlternativeEntryRepository repository, Clock clock,
                               PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.clock = clock;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 바꿀 음식에 저장된 대안 중 칼로리 차이가 tolerance 안인 것. 먼저 저장된 순서. */
    public List<FoodItem> find(FoodItem target, double tolerance) {
        int allowed = (int) Math.max(target.getCalories() * tolerance, 20);
        List<FoodItem> found = readOnlyTransaction.execute(status -> repository
                .findBySourceKeyAndCaloriesBetweenOrderByIdAsc(
                        key(target.getName()), target.getCalories() - allowed, target.getCalories() + allowed)
                .stream()
                .map(FoodAlternativeEntry::toFoodItem)
                .toList());
        return found == null ? List.of() : found;
    }

    /** 형식이 맞고 칼로리 차이가 tolerance 안인 후보만 저장한다. 이미 있는 대안은 건너뛴다. */
    public void save(FoodItem target, List<FoodItem> candidates, double tolerance) {
        String sourceKey = key(target.getName());
        int allowed = (int) Math.max(target.getCalories() * tolerance, 20);
        for (FoodItem candidate : candidates == null ? List.<FoodItem>of() : candidates) {
            if (!MealPlanValidator.isWellFormed(candidate)
                    || Math.abs(candidate.getCalories() - target.getCalories()) > allowed) {
                continue;
            }
            String candidateKey = key(candidate.getName());
            if (candidateKey.isEmpty() || candidateKey.equals(sourceKey)) {
                continue;
            }
            FoodItem trimmed = FoodItem.builder()
                    .name(candidate.getName().trim())
                    .amount(candidate.getAmount().trim())
                    .calories(candidate.getCalories())
                    .protein(candidate.getProtein())
                    .carbohydrate(candidate.getCarbohydrate())
                    .fat(candidate.getFat())
                    .shoppingKeyword(candidate.getShoppingKeyword().isBlank()
                            ? candidate.getName().trim() : candidate.getShoppingKeyword().trim())
                    .build();
            try {
                newTransaction.executeWithoutResult(status -> {
                    if (!repository.existsBySourceKeyAndCandidateKey(sourceKey, candidateKey)) {
                        repository.save(new FoodAlternativeEntry(sourceKey, candidateKey, trimmed, LocalDateTime.now(clock)));
                    }
                });
            } catch (DataIntegrityViolationException e) {
                // 다른 요청이 같은 대안을 먼저 저장했다
                log.debug("이미 저장된 대안. source={}, candidate={}", sourceKey, candidateKey);
            }
        }
    }

    /** 이름 비교용 키. 공백을 빼고 소문자로 바꾼다. */
    static String key(String name) {
        return name == null ? "" : name.replace(" ", "").toLowerCase(Locale.ROOT);
    }
}
