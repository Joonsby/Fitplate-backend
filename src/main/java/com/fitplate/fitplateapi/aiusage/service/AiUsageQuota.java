// 사용자별 하루 AI 호출 횟수를 세고, 상한을 넘으면 막습니다. 호출이 실패하면 한 번을 돌려줍니다.
package com.fitplate.fitplateapi.aiusage.service;

import com.fitplate.fitplateapi.aiusage.domain.AiDailyUsage;
import com.fitplate.fitplateapi.aiusage.domain.AiFeature;
import com.fitplate.fitplateapi.aiusage.repository.AiDailyUsageRepository;
import com.fitplate.fitplateapi.exception.AiDailyLimitExceededException;
import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class AiUsageQuota {

    private final AiDailyUsageRepository usageRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final TransactionTemplate newTransaction;

    public AiUsageQuota(AiDailyUsageRepository usageRepository, UserRepository userRepository, Clock clock,
                        PlatformTransactionManager transactionManager) {
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 오늘 횟수를 하나 쓴다. 이미 다 썼으면 429 예외. */
    public void acquire(String tossUserKey, AiFeature feature) {
        try {
            newTransaction.executeWithoutResult(status -> increase(tossUserKey, feature));
        } catch (DataIntegrityViolationException e) {
            // 같은 사용자의 첫 요청 두 개가 동시에 줄을 만들었다. 이제 줄이 있으니 한 번 더 시도한다
            newTransaction.executeWithoutResult(status -> increase(tossUserKey, feature));
        }
    }

    /** AI 호출이 실패해 결과를 못 준 경우 쓴 횟수를 돌려준다. */
    public void release(String tossUserKey, AiFeature feature) {
        newTransaction.executeWithoutResult(status -> {
            User user = findUser(tossUserKey);
            usageRepository.findByUserAndUsageDateAndFeature(user, today(), feature)
                    .ifPresent(AiDailyUsage::decrease);
        });
    }

    /** 오늘 이미 쓴 횟수. */
    public int usedToday(String tossUserKey, AiFeature feature) {
        Integer used = newTransaction.execute(status -> usageRepository
                .findByUserAndUsageDateAndFeature(findUser(tossUserKey), today(), feature)
                .map(AiDailyUsage::getUsedCount)
                .orElse(0));
        return used == null ? 0 : used;
    }

    private void increase(String tossUserKey, AiFeature feature) {
        User user = findUser(tossUserKey);
        LocalDate today = today();
        AiDailyUsage usage = usageRepository.findByUserAndUsageDateAndFeature(user, today, feature)
                .orElseGet(() -> usageRepository.saveAndFlush(new AiDailyUsage(user, today, feature)));
        if (usage.getUsedCount() >= feature.dailyLimit()) {
            throw new AiDailyLimitExceededException(limitMessage(feature));
        }
        usage.increase();
    }

    private static String limitMessage(AiFeature feature) {
        return switch (feature) {
            case GENERATION -> "오늘은 식단을 더 만들 수 없어요. 저장한 식단을 다른 날에 다시 쓰거나 끼니를 바꿔 보세요.";
            case ALTERNATIVES -> "오늘은 새 대안을 더 찾을 수 없어요. 내일 다시 시도해 주세요.";
            case MEAL_REGENERATION -> "오늘은 끼니를 더 다시 만들 수 없어요. 내일 다시 시도해 주세요.";
        };
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private User findUser(String tossUserKey) {
        return userRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new ResourceNotFoundException(tossUserKey, "사용자를 찾을 수 없습니다"));
    }
}
