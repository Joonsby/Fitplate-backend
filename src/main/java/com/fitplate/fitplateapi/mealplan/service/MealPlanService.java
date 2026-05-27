package com.fitplate.fitplateapi.mealplan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.ai.GeminiMealPlanClient;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.mealplan.dto.*;
import com.fitplate.fitplateapi.mealplan.repository.MealPlanRepository;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import com.fitplate.fitplateapi.user.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service  // @Component의 특수한 형태. 비즈니스 로직 담당
public class MealPlanService {
    private final GeminiMealPlanClient geminiMealPlanClient;
    private final UserRepository userRepository;
    private final MealPlanRepository mealPlanRepository;
    private final ObjectMapper objectMapper;
    private final UserProfileService userProfileService;

    public MealPlanService(
            GeminiMealPlanClient geminiMealPlanClient,
            UserRepository userRepository,
            MealPlanRepository mealPlanRepository,
            ObjectMapper objectMapper,
            UserProfileService userProfileService
    ) {
        this.geminiMealPlanClient = geminiMealPlanClient;
        this.userRepository = userRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.objectMapper = objectMapper;
        this.userProfileService = userProfileService;
    }

    @Transactional
    public MealPlanGenerateResponse generateMealPlan(MealPlanRequest request) {
        // 1. 사용자 프로필 저장/수정
        userProfileService.upsertFromMealPlanRequest(request);

        // 2. 영양 계산
        NutritionResult nutritionResult = calculateNutrition(
                request.getHeight(),
                request.getWeight(),
                request.getAge(),
                request.getBodyFatRate(),
                request.getGender(),
                request.getGoal()
        );
        MealPlanResponse aiMealPlanResponse = geminiMealPlanClient.generateMealPlan(request);

        return MealPlanGenerateResponse.builder()
                .height(request.getHeight())
                .weight(request.getWeight())
                .age(request.getAge())
                .gender(request.getGender())
                .goal(request.getGoal())
                .periodDays(request.getPeriodDays())
                .targetCalories(nutritionResult.getTargetCalories())
                .bmr(nutritionResult.getBmr())
                .tdee(nutritionResult.getTdee())
                .proteinGram(nutritionResult.getProteinGram())
                .carbsGram(nutritionResult.getCarbsGram())
                .fatGram(nutritionResult.getFatGram())
                .aiMealPlanResponse(aiMealPlanResponse)
                .build();
    }

    @Transactional
    public void saveMealPlan(SaveMealPlanRequest request) {
        User user = userRepository.findByTossUserKey(request.getTossUserKey())
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다: " + request.getTossUserKey()
                ));

        UserProfile profile = userProfileService.findByTossUserKey(request.getTossUserKey());

        Double bodyFatRate = profile.getBodyFatRate() == null
                ? null
                : profile.getBodyFatRate().doubleValue();

        NutritionResult nutrition = calculateNutrition(
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getAge(),
                bodyFatRate,
                profile.getGender(),
                request.getGoal()
        );

        LocalDateTime now = LocalDateTime.now();

        MealPlan mealPlan = MealPlan.builder()
                .user(user)
                .goal(request.getGoal())
                .durationDays(request.getPeriodDays())
                .heightCm(profile.getHeightCm())
                .weightKg(profile.getWeightKg())
                .age(profile.getAge())
                .gender(profile.getGender())
                .bmi(profile.getBmi())
                .bodyFatRate(profile.getBodyFatRate())
                .targetCalories(nutrition.getTargetCalories())
                .bmr(nutrition.getBmr())
                .tdee(nutrition.getTdee())
                .proteinGram(nutrition.getProteinGram())
                .carbsGram(nutrition.getCarbsGram())
                .fatGram(nutrition.getFatGram())
                .aiResponseJson(request.getAiMealPlanResponse().toString())
                .startedAt(now)
                .expiresAt(now.plusDays(request.getPeriodDays()))
                .build();

        mealPlanRepository.save(mealPlan);
    }

    @Transactional(readOnly = true)
    public List<SavedMealPlanResponse> getSavedMealPlans(String tossUserKey) {
        User user = userRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + tossUserKey));

        return mealPlanRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(SavedMealPlanResponse::from)
                .toList();
    }

    /**
     * 특정 ID의 식단 상세 정보를 조회합니다
     *
     * @param mealPlanId 조회할 식단의 ID
     * @return 식단 상세 정보 (저장된 AI 응답 포함)
     * @throws IllegalArgumentException 식단을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public MealPlanDetailResponse findById(Long mealPlanId) {
        return mealPlanRepository.findById(mealPlanId)
                .map(mealPlan -> MealPlanDetailResponse.from(mealPlan, objectMapper))
                .orElseThrow(() -> new IllegalArgumentException("식단을 찾을 수 없습니다: " + mealPlanId));
    }
    private NutritionResult calculateNutrition(
            Integer height,
            Integer weight,
            Integer age,
            Double bodyFatRate,
            String gender,
            String goal
    ) {
        int bmr = calculateBmr(
                height,
                weight,
                age,
                bodyFatRate,
                gender
        );

        int tdee = calculateTdee(bmr);
        int targetCalories = calculateTargetCalories(tdee, goal);

        int proteinGram = calculateProteinGram(weight);
        int fatGram = calculateFatGram(targetCalories);
        int carbsGram = calculateCarbsGram(
                targetCalories,
                proteinGram,
                fatGram
        );

        return NutritionResult.builder()
                .bmr(bmr)
                .tdee(tdee)
                .targetCalories(targetCalories)
                .proteinGram(proteinGram)
                .fatGram(fatGram)
                .carbsGram(carbsGram)
                .build();
    }

    private int calculateBmr(
            Integer height,
            Integer weight,
            Integer age,
            Double bodyFatRate,
            String gender
    ) {

        // 체지방률이 존재하면 Katch-McArdle 공식 사용
        if (bodyFatRate != null) {

            // 제지방량(LBM)
            double leanBodyMass =
                    weight * (1 - (bodyFatRate / 100.0));

            return (int) Math.round(
                    370 + (21.6 * leanBodyMass)
            );
        }

        // 체지방률 없으면 기존 공식 fallback
        if ("MALE".equalsIgnoreCase(gender)) {
            return (int) Math.round(
                    10 * weight
                            + 6.25 * height
                            - 5 * age
                            + 5
            );
        }

        return (int) Math.round(
                10 * weight
                        + 6.25 * height
                        - 5 * age
                        - 161
        );
    }

    private int calculateTdee(int bmr) {
        return (int) Math.round(bmr * 1.35);
    }

    private int calculateTargetCalories(
            int tdee,
            String goal
    ) {
        return switch (goal) {
            case "WEIGHT_LOSS" -> tdee - 400;
            case "MUSCLE_GAIN" -> tdee + 300;
            default -> tdee;
        };
    }

    private int calculateProteinGram(Integer weight) {
        return (int) Math.round(weight * 1.6);
    }

    private int calculateFatGram(int targetCalories) {
        return (int) Math.round(
                (targetCalories * 0.25) / 9
        );
    }

    private int calculateCarbsGram(
            int targetCalories,
            int proteinGram,
            int fatGram
    ) {
        int proteinCalories = proteinGram * 4;
        int fatCalories = fatGram * 9;

        return (
                targetCalories
                        - proteinCalories
                        - fatCalories
        ) / 4;
    }
}