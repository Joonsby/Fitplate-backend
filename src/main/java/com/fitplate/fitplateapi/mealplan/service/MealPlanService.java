package com.fitplate.fitplateapi.mealplan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.ai.GeminiMealPlanClient;
import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.exception.DuplicateMealPlanException;
import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.mealplan.dto.*;
import com.fitplate.fitplateapi.mealplan.repository.MealPlanRepository;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.nutrition.service.NutritionCalculator;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import com.fitplate.fitplateapi.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Service  // @Component의 특수한 형태. 비즈니스 로직 담당
@RequiredArgsConstructor
public class MealPlanService {
    private final GeminiMealPlanClient geminiMealPlanClient;
    private final UserRepository userRepository;
    private final MealPlanRepository mealPlanRepository;
    private final ObjectMapper objectMapper;
    private final UserProfileService userProfileService;
    private final UserProfileRepository userProfileRepository;
    private final NutritionCalculator nutritionCalculator;

    @Transactional
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
    public long saveMealPlan(String tossUserKey, SaveMealPlanRequest request) {
        User user = userRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new ResourceNotFoundException(tossUserKey, "사용자를 찾을 수 없습니다"));

        UserProfile profile = userProfileRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new ResourceNotFoundException(tossUserKey, "사용자 프로필을 찾을 수 없습니다"));

        String aiResponseJson = request.getAiMealPlanResponse().toString();
        String aiResponseHash = sha256(aiResponseJson);

        boolean alreadySaved = mealPlanRepository.existsByUserAndAiResponseHash(
                user,
                aiResponseHash
        );

        if (alreadySaved) {
            throw new DuplicateMealPlanException();
        }

        Double bodyFatRate = profile.getBodyFatRate() == null
                ? null
                : profile.getBodyFatRate().doubleValue();

        NutritionResult nutrition = nutritionCalculator.calculate(
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
                .aiResponseJson(aiResponseJson)
                .aiResponseHash(aiResponseHash)
                .startedAt(now)
                .expiresAt(now.plusDays(request.getPeriodDays()))
                .build();

        mealPlanRepository.save(mealPlan);

        return mealPlan.getMealPlanId();
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
                .orElseThrow(() -> new ResourceNotFoundException(mealPlanId, "식단을 찾을 수 없습니다"));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시 생성 실패", e);
        }
    }
}