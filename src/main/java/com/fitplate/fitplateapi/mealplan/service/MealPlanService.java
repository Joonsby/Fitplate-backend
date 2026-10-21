package com.fitplate.fitplateapi.mealplan.service;

import com.fitplate.fitplateapi.ai.MealPlanAiClient;
import com.fitplate.fitplateapi.ai.PersonalizationHints;
import com.fitplate.fitplateapi.aiusage.domain.AiFeature;
import com.fitplate.fitplateapi.aiusage.service.AiUsageQuota;
import com.fitplate.fitplateapi.exception.AllergySafeResultUnavailableException;
import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.mealplan.domain.MealPlanNamer;
import com.fitplate.fitplateapi.mealplan.dto.*;
import com.fitplate.fitplateapi.mealplan.repository.MealPlanRepository;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.nutrition.service.NutritionCalculator;
import com.fitplate.fitplateapi.personalization.service.PersonalizationService;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
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
    private final MealPlanValidator mealPlanValidator;
    private final PersonalizationService personalizationService;
    private final AiUsageQuota aiUsageQuota;

    public MealPlanGenerateResponse generateMealPlan(String tossUserKey,MealPlanRequest request) {
        // 1. 사용자 프로필 저장/수정
        userProfileService.upsertFromMealPlanRequest(tossUserKey,request);
        FoodPreferences preferences = userProfileService.getFoodPreferences(tossUserKey);
        PersonalizationHints hints = personalizationService.hintsFor(tossUserKey, preferences);

        // 2. 영양 계산
        NutritionResult nutritionResult = nutritionCalculator.calculate(
                request.getHeight(),
                request.getWeight(),
                request.getAge(),
                request.getBodyFatRate(),
                request.getGender(),
                request.getGoal()
        );

        //3. 식단 생성 (검증 실패 시 한 번 재요청). 하루 상한을 넘으면 429, 실패하면 횟수를 돌려준다
        aiUsageQuota.acquire(tossUserKey, AiFeature.GENERATION);
        MealPlanResponse aiMealPlanResponse;
        Long mealPlanId;
        try {
            aiMealPlanResponse = generateValidated(request, nutritionResult, preferences, hints);

            //4. 식단 저장
            mealPlanId = mealPlanStorageService.saveGeneratedMealPlan(tossUserKey,request,nutritionResult,aiMealPlanResponse);
        } catch (RuntimeException e) {
            aiUsageQuota.release(tossUserKey, AiFeature.GENERATION);
            throw e;
        }

        return MealPlanGenerateResponse.builder()
                .mealPlanId(mealPlanId)
                .title(MealPlanNamer.defaultName(request.getGoal(), aiMealPlanResponse))
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

    /**
     * AI 결과가 형식·목표·제외 조건을 어기면 문제점을 알려주며 한 번 더 요청한다.
     * 두 번 다 어기면 쓸 수 있는 것 중 목표에 더 가까운 쪽을 쓴다. 알레르기 음식이 든 결과는 쓰지 않는다.
     */
    MealPlanResponse generateValidated(MealPlanRequest request, NutritionResult nutritionResult,
                                       FoodPreferences preferences, PersonalizationHints hints) {
        MealPlanResponse first = mealPlanAiClient.generateMealPlan(request, nutritionResult, preferences, hints, null);
        MealPlanValidator.Result firstResult = mealPlanValidator.validate(first, nutritionResult, preferences);
        if (firstResult.isAcceptable()) {
            return first;
        }
        log.warn("AI 식단 검증 실패, 재요청. problems={}", firstResult.problems());

        MealPlanResponse second;
        try {
            second = mealPlanAiClient.generateMealPlan(request, nutritionResult, preferences, hints, firstResult.feedback());
        } catch (RuntimeException e) {
            if (firstResult.isUsable()) {
                log.warn("AI 식단 재요청 실패, 첫 결과 사용. cause={}", e.getMessage());
                return first;
            }
            if (firstResult.allergyViolation()) {
                throw new AllergySafeResultUnavailableException();
            }
            throw e;
        }
        MealPlanValidator.Result secondResult = mealPlanValidator.validate(second, nutritionResult, preferences);
        if (secondResult.isAcceptable()) {
            return second;
        }
        if (!firstResult.isUsable() && !secondResult.isUsable()) {
            if (firstResult.allergyViolation() || secondResult.allergyViolation()) {
                throw new AllergySafeResultUnavailableException();
            }
            throw new IllegalStateException("AI가 올바른 형식의 식단을 만들지 못했습니다. 잠시 후 다시 시도해주세요.");
        }
        log.warn("AI 식단 재요청 후에도 검증 실패. problems={}", secondResult.problems());
        if (!firstResult.isUsable()) {
            return second;
        }
        if (!secondResult.isUsable()) {
            return first;
        }
        return secondResult.score() <= firstResult.score() ? second : first;
    }

    /** 오늘 식단을 몇 번 더 만들 수 있는지와 다음 생성에 광고가 필요한지. */
    public GenerationQuotaResponse getGenerationQuota(String tossUserKey) {
        return GenerationQuotaResponse.of(
                aiUsageQuota.usedToday(tossUserKey, AiFeature.GENERATION), AiFeature.GENERATION.dailyLimit());
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
