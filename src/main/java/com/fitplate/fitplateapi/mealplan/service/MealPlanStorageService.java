// 식단과 끼니 및 음식을 하나의 트랜잭션으로 저장합니다.
package com.fitplate.fitplateapi.mealplan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.mealplan.domain.MealPlanFood;
import com.fitplate.fitplateapi.mealplan.domain.MealPlanMeal;
import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import com.fitplate.fitplateapi.mealplan.dto.MealResponse;
import com.fitplate.fitplateapi.mealplan.repository.MealPlanRepository;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class MealPlanStorageService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final MealPlanRepository mealPlanRepository;
    private final ObjectMapper objectMapper;

    /** 저장한 식단 ID 를 돌려준다. */
    @Transactional
    public Long saveGeneratedMealPlan(String tossUserKey, MealPlanRequest request,
                                      NutritionResult nutritionResult, MealPlanResponse response) {
        User user = userRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new ResourceNotFoundException(tossUserKey, "사용자를 찾을 수 없습니다"));
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(user.getUserId(), "사용자 프로필을 찾을 수 없습니다"));

        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 식단 응답 JSON 변환 실패", e);
        }

        MealPlan plan = MealPlan.builder()
                .user(user)
                .goal(request.getGoal())
                .height(request.getHeight())
                .weight(request.getWeight())
                .age(request.getAge())
                .gender(request.getGender())
                .bmi(profile.getBmi())
                .bodyFatRate(profile.getBodyFatRate())
                .targetCalories(nutritionResult.getTargetCalories())
                .bmr(nutritionResult.getBmr())
                .tdee(nutritionResult.getTdee())
                .proteinGram(nutritionResult.getProteinGram())
                .carbsGram(nutritionResult.getCarbsGram())
                .fatGram(nutritionResult.getFatGram())
                .aiResponseJson(responseJson)
                .build();

        for (int mealIndex = 0; mealIndex < response.getMeals().size(); mealIndex++) {
            MealResponse sourceMeal = response.getMeals().get(mealIndex);
            MealPlanMeal meal = MealPlanMeal.builder()
                    .mealType(sourceMeal.getMealType())
                    .title(sourceMeal.getTitle())
                    .sortOrder(mealIndex)
                    .build();
            plan.addMeal(meal);

            for (int foodIndex = 0; foodIndex < sourceMeal.getFoods().size(); foodIndex++) {
                FoodItem sourceFood = sourceMeal.getFoods().get(foodIndex);
                meal.addFood(MealPlanFood.builder()
                        .name(sourceFood.getName())
                        .amount(sourceFood.getAmount())
                        .calories(sourceFood.getCalories())
                        .protein(toDecimal(sourceFood.getProtein()))
                        .carbohydrate(toDecimal(sourceFood.getCarbohydrate()))
                        .fat(toDecimal(sourceFood.getFat()))
                        .shoppingKeyword(sourceFood.getShoppingKeyword())
                        .sortOrder(foodIndex)
                        .build());
            }
        }
        return mealPlanRepository.save(plan).getId();
    }

    private BigDecimal toDecimal(Double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
