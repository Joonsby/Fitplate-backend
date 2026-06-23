package com.fitplate.fitplateapi.mealplan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanService {
    private final GeminiMealPlanClient geminiMealPlanClient;
    private final UserRepository userRepository;
    private final MealPlanRepository mealPlanRepository;
    private final ObjectMapper objectMapper;
    private final UserProfileService userProfileService;
    private final UserProfileRepository userProfileRepository;
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
        MealPlanResponse aiMealPlanResponse = geminiMealPlanClient.generateMealPlan(request, nutritionResult);

        //4. 식단 저장
        saveGeneratedMealPlan(tossUserKey,request,nutritionResult,aiMealPlanResponse);

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

    @Transactional
    private void saveGeneratedMealPlan(
            String tossUserKey,
            MealPlanRequest request,
            NutritionResult nutritionResult,
            MealPlanResponse aiMealPlanResponse
    ){
        //1. 사용자 조회
        User user = userRepository.findByTossUserKey(tossUserKey).orElseThrow(() -> new ResourceNotFoundException(tossUserKey, "사용자를 찾을 수 없습니다"));

        //2. 사용자 프로필 조회
        UserProfile profile = userProfileRepository.findByUser(user).orElseThrow(() -> new ResourceNotFoundException(user.getUserId(),"사용자 프로필을 찾을 수 없습니다"));

        //3. AI 식단 응답 JSON 변환
        String aiResponseJson;
        try{
            aiResponseJson = objectMapper.writeValueAsString(aiMealPlanResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 식단 응답 JSON 변환 실패",e);
        }

        //4. 식단 계획 생성
        MealPlan mealPlan = MealPlan.builder()
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
                .aiResponseJson(aiResponseJson)
                .startedAt(LocalDateTime.now())
                .build();

        //5. 식단 계획 저장
        mealPlanRepository.save(mealPlan);
    }

    @Transactional(readOnly = true)
    public List<SavedMealPlanResponse> getSavedMealPlans(String tossUserKey) {
        User user = userRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new ResourceNotFoundException(tossUserKey, "사용자를 찾을 수 없습니다"));

        return mealPlanRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(mealPlan -> SavedMealPlanResponse.from(mealPlan, objectMapper))
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