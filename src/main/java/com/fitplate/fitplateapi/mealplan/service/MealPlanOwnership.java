// 요청한 사용자 본인의 식단·끼니인지 확인해 꺼내 줍니다. 다른 사용자의 식단이면 403 입니다.
package com.fitplate.fitplateapi.mealplan.service;

import com.fitplate.fitplateapi.exception.MealPlanAccessDeniedException;
import com.fitplate.fitplateapi.exception.MealPlanConflictException;
import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.mealplan.domain.MealPlanMeal;
import com.fitplate.fitplateapi.mealplan.repository.MealPlanRepository;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MealPlanOwnership {

    private final UserRepository userRepository;
    private final MealPlanRepository mealPlanRepository;

    public User findUser(String tossUserKey) {
        return userRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new ResourceNotFoundException(tossUserKey, "사용자를 찾을 수 없습니다"));
    }

    public MealPlan findOwnedPlan(String tossUserKey, Long mealPlanId) {
        User user = findUser(tossUserKey);
        MealPlan plan = mealPlanRepository.findById(mealPlanId)
                .orElseThrow(() -> new ResourceNotFoundException(mealPlanId, "식단을 찾을 수 없습니다"));
        if (!plan.getUser().getUserId().equals(user.getUserId())) {
            throw new MealPlanAccessDeniedException();
        }
        return plan;
    }

    public MealPlanMeal findMeal(MealPlan plan, String mealType) {
        return plan.getMeals().stream()
                .filter(candidate -> candidate.getMealType().equals(mealType))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(mealType, "해당 끼니를 찾을 수 없습니다"));
    }

    /** 먹은 끼니는 음식을 바꾸거나 다시 만들 수 없다 (통계가 먹은 음식과 어긋나지 않도록). */
    public void requireNotEaten(MealPlanMeal meal) {
        if (meal.isEaten()) {
            throw new MealPlanConflictException(MealPlanConflictException.MEAL_EATEN,
                    "먹었다고 체크한 끼니는 바꿀 수 없어요. 체크를 해제한 뒤 바꿔 주세요.");
        }
    }
}
