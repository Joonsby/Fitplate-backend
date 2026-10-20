package com.fitplate.fitplateapi.mealplan.service;

import com.fitplate.fitplateapi.ai.MealPlanAiClient;
import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.mealplan.dto.*;
import com.fitplate.fitplateapi.mealplan.repository.MealPlanRepository;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.nutrition.service.NutritionCalculator;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import com.fitplate.fitplateapi.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanService {
    private final MealPlanAiClient mealPlanAiClient;
    private final UserRepository userRepository;
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanStorageService mealPlanStorageService;
    private final UserProfileService userProfileService;
    private final NutritionCalculator nutritionCalculator;

    public MealPlanGenerateResponse generateMealPlan(String tossUserKey,MealPlanRequest request) {
        // 1. 사용자 프로필 저장/수정
        userProfileService.upsertFromMealPlanRequest(tossUserKey,request);

        // 2. 영양 계산
        NutritionResult nutritionResult = nutritionCalculator.calculate(
                request.getHeight(),
                request.getWeight(),
                request.getAge(),
                request.getBodyFatRate(),
                request.getGender(),
                request.getGoal()
        );

        //3. 식단 생성
        MealPlanResponse aiMealPlanResponse = mealPlanAiClient.generateMealPlan(request, nutritionResult);

        //4. 식단 저장
        mealPlanStorageService.saveGeneratedMealPlan(tossUserKey,request,nutritionResult,aiMealPlanResponse);

        return MealPlanGenerateResponse.builder()
                .height(request.getHeight())
                .weight(request.getWeight())
                .age(request.getAge())
                .gender(request.getGender())
                .goal(request.getGoal())
                .targetCalories(nutritionResult.getTargetCalories())
                .bmr(nutritionResult.getBmr())
                .tdee(nutritionResult.getTdee())
                .proteinGram(nutritionResult.getProteinGram())
                .carbsGram(nutritionResult.getCarbsGram())
                .fatGram(nutritionResult.getFatGram())
                .aiMealPlanResponse(aiMealPlanResponse)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SavedMealPlanResponse> getSavedMealPlans(String tossUserKey) {
        User user = userRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new ResourceNotFoundException(tossUserKey, "사용자를 찾을 수 없습니다"));

        return mealPlanRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(SavedMealPlanResponse::from)
                .toList();
    }

    @Transactional
    public void deleteMealPlan(String tossUserKey, Long mealPlanId) {
        User user = userRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new ResourceNotFoundException(tossUserKey, "사용자를 찾을 수 없습니다"));

        MealPlan mealPlan = mealPlanRepository.findById(mealPlanId)
                .orElseThrow(() -> new ResourceNotFoundException(mealPlanId, "식단을 찾을 수 없습니다"));

        log.info("user : {}, mealPlan.user : {}", user, mealPlan.getUser());
        if (!mealPlan.getUser().equals(user)) {
            throw new IllegalArgumentException("해당 식단에 대한 삭제 권한이 없습니다");
        }

        mealPlanRepository.delete(mealPlan);
    }

    /** 식단 상세 조회 (저장된 AI 응답 포함). */
    @Transactional(readOnly = true)
    public MealPlanDetailResponse findById(Long mealPlanId) {
        return mealPlanRepository.findById(mealPlanId)
                .map(MealPlanDetailResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(mealPlanId, "식단을 찾을 수 없습니다"));
    }
}
