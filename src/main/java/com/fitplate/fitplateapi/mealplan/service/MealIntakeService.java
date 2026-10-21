// 식단을 날짜에 지정하고 끼니별 섭취 여부를 기록합니다.
package com.fitplate.fitplateapi.mealplan.service;

import com.fitplate.fitplateapi.exception.InvalidRequestException;
import com.fitplate.fitplateapi.exception.MealPlanConflictException;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.mealplan.domain.MealPlanMeal;
import com.fitplate.fitplateapi.mealplan.dto.SavedMealPlanResponse;
import com.fitplate.fitplateapi.mealplan.repository.MealPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MealIntakeService {

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanOwnership ownership;
    private final Clock clock;

    /**
     * 식단을 날짜에 지정한다. planDate 가 null 이면 지정을 해제한다.
     * 같은 날짜에 다른 식단이 있으면 replace=true 일 때만 그 식단의 지정을 풀고 바꾼다.
     * 먹은 끼니가 있는 식단은 날짜를 바꾸거나 풀 수 없다 (섭취 기록이 다른 날로 옮겨지지 않도록).
     */
    @Transactional
    public SavedMealPlanResponse schedule(String tossUserKey, Long mealPlanId, LocalDate planDate, boolean replace) {
        MealPlan plan = ownership.findOwnedPlan(tossUserKey, mealPlanId);
        if (planDate != null && planDate.equals(plan.getPlanDate())) {
            return SavedMealPlanResponse.from(plan);
        }
        if (plan.getPlanDate() != null && plan.hasEatenMeal()) {
            throw new MealPlanConflictException(MealPlanConflictException.HAS_INTAKE,
                    "먹었다고 체크한 끼니가 있어 날짜를 바꿀 수 없어요.");
        }

        if (planDate != null) {
            releaseDate(plan, planDate, replace);
        }
        plan.schedule(planDate);
        return SavedMealPlanResponse.from(plan);
    }

    /**
     * 같은 음식으로 새 식단을 만들어 다른 날짜에 지정한다. 원래 식단은 그대로 둔다.
     * 그날 다른 식단이 있으면 schedule 과 같은 규칙(DATE_TAKEN·replace)을 따른다.
     */
    @Transactional
    public SavedMealPlanResponse copyToDate(String tossUserKey, Long mealPlanId, LocalDate planDate, boolean replace) {
        if (planDate == null) {
            throw new InvalidRequestException("식단을 먹을 날짜를 골라 주세요.");
        }
        MealPlan source = ownership.findOwnedPlan(tossUserKey, mealPlanId);
        if (planDate.equals(source.getPlanDate())) {
            throw new InvalidRequestException("이미 그날 먹는 식단이에요. 다른 날짜를 골라 주세요.");
        }
        releaseDate(source, planDate, replace);
        MealPlan copy = source.copy();
        copy.schedule(planDate);
        return SavedMealPlanResponse.from(mealPlanRepository.save(copy));
    }

    /** planDate 를 쓰고 있는 다른 식단이 있으면 replace 일 때만 그 지정을 푼다. */
    private void releaseDate(MealPlan plan, LocalDate planDate, boolean replace) {
        Optional<MealPlan> occupant = mealPlanRepository.findByUserAndPlanDate(plan.getUser(), planDate);
        if (occupant.isEmpty() || occupant.get().getId().equals(plan.getId())) {
            return;
        }
        MealPlan other = occupant.get();
        if (!replace) {
            throw new MealPlanConflictException(MealPlanConflictException.DATE_TAKEN,
                    "이 날짜에는 이미 다른 식단이 지정돼 있어요.");
        }
        if (other.hasEatenMeal()) {
            throw new MealPlanConflictException(MealPlanConflictException.HAS_INTAKE,
                    "이 날짜의 식단에 먹었다고 체크한 끼니가 있어 바꿀 수 없어요.");
        }
        other.schedule(null);
        // 같은 날짜 유니크 키가 겹치지 않도록 먼저 반영한다
        mealPlanRepository.flush();
    }

    /** 끼니를 먹었는지 기록한다. 같은 값을 다시 보내도 결과가 같다 (중복 요청에 안전). */
    @Transactional
    public SavedMealPlanResponse setIntake(String tossUserKey, Long mealPlanId, String mealType, boolean eaten) {
        MealPlan plan = ownership.findOwnedPlan(tossUserKey, mealPlanId);
        if (plan.getPlanDate() == null) {
            throw new MealPlanConflictException(MealPlanConflictException.NOT_SCHEDULED,
                    "먼저 이 식단을 기록할 날짜를 정해 주세요.");
        }
        LocalDate today = LocalDate.now(clock);
        if (eaten && plan.getPlanDate().isAfter(today)) {
            throw new MealPlanConflictException(MealPlanConflictException.FUTURE_DATE,
                    "아직 오지 않은 날짜의 끼니는 체크할 수 없어요.");
        }
        MealPlanMeal meal = ownership.findMeal(plan, mealType);
        if (eaten) {
            meal.markEaten(LocalDateTime.now(clock));
        } else {
            meal.clearEaten();
        }
        return SavedMealPlanResponse.from(plan);
    }
}
