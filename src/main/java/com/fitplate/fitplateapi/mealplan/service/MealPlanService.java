package com.fitplate.fitplateapi.mealplan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.ai.GeminiMealPlanClient;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.mealplan.dto.*;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.mealplan.repository.MealPlanRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 식단 생성 비즈니스 로직을 담당하는 Service 클래스
 *
 * 📌 Service 계층의 역할:
 * - 비즈니스 로직 실행 (식단 생성, 데이터 계산 등)
 * - Repository를 통해 DB와 상호작용
 * - 외부 API 호출 (Gemini AI)
 * - 트랜잭션 관리
 *
 * 📌 generateMealPlan() 실행 흐름:
 * 1. 사용자 신체 정보로부터 칼로리 계산
 *    - BMR(기초대사량) 계산
 *    - TDEE(일일 총 소비 칼로리) 계산
 *    - 목표에 따른 목표 칼로리 조정
 * 2. 영양소 분배 계산
 *    - 단백질, 탄수화물, 지방 그램 계산
 * 3. AI 모델(Gemini)에 요청하여 식단 생성
 * 4. 사용자 정보 저장 (없으면 생성)
 * 5. 생성된 식단을 MealPlan 엔티티로 저장
 * 6. 클라이언트에 응답
 */
@Service  // @Component의 특수한 형태. 비즈니스 로직 담당
public class MealPlanService {

    private static final String MOCK_USER_KEY = "MOCK_USER_001";
    private final GeminiMealPlanClient geminiMealPlanClient;
    private final UserRepository userRepository;
    private final MealPlanRepository mealPlanRepository;
    private final ObjectMapper objectMapper;

    public MealPlanService(
            GeminiMealPlanClient geminiMealPlanClient,
            UserRepository userRepository,
            MealPlanRepository mealPlanRepository,
            ObjectMapper objectMapper
    ) {
        this.geminiMealPlanClient = geminiMealPlanClient;
        this.userRepository = userRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
//    public MealPlanGenerateResponse saveMealPlan(SaveMealPlanRequest request) {
//        int bmr = calculateBmr(request);
//        int tdee = calculateTdee(bmr);
//        int targetCalories = calculateTargetCalories(tdee, request.getGoal());
//        int proteinGram = calculateProteinGram(request);
//        int fatGram = calculateFatGram(targetCalories);
//        int carbsGram = calculateCarbsGram(targetCalories, proteinGram, fatGram);
//
//        MealPlanResponse aiMealPlanResponse =
//                geminiMealPlanClient.generateMealPlan(request);
//
//        return MealPlanGenerateResponse.builder()
//                .height(request.getHeight())
//                .weight(request.getWeight())
//                .age(request.getAge())
//                .gender(request.getGender())
//                .bodyFatRate(request.getBodyFatRate())
//                .goal(request.getGoal())
//                .periodDays(request.getPeriodDays())
//                .targetCalories(targetCalories)
//                .bmr(bmr)
//                .tdee(tdee)
//                .proteinGram(proteinGram)
//                .carbsGram(carbsGram)
//                .fatGram(fatGram)
//                .aiMealPlanResponse(aiMealPlanResponse)
//                .build();
//    }

    /**
     * 사용자 정보로부터 맞춤형 식단을 생성합니다
     *
     * @param request 사용자의 신체 정보 및 목표
     * @return AI가 생성한 식단 계획
     */
    public MealPlanResponse generateMealPlan(MealPlanRequest request) {
        return geminiMealPlanClient.generateMealPlan(request);
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

    private int calculateBmr(MealPlanRequest request) {
        if ("MALE".equalsIgnoreCase(request.getGender())) {
            return (int) Math.round(
                    10 * request.getWeight()
                            + 6.25 * request.getHeight()
                            - 5 * request.getAge()
                            + 5
            );
        }

        return (int) Math.round(
                10 * request.getWeight()
                        + 6.25 * request.getHeight()
                        - 5 * request.getAge()
                        - 161
        );
    }

    private int calculateTdee(int bmr) {
        return (int) Math.round(bmr * 1.35);
    }

    private int calculateTargetCalories(int tdee, String goal) {
        return switch (goal) {
            case "WEIGHT_LOSS" -> tdee - 400;
            case "MUSCLE_GAIN" -> tdee + 300;
            default -> tdee;
        };
    }

    private int calculateProteinGram(MealPlanRequest request) {
        return (int) Math.round(request.getWeight() * 1.6);
    }

    private int calculateFatGram(int targetCalories) {
        return (int) Math.round((targetCalories * 0.25) / 9);
    }

    private int calculateCarbsGram(
            int targetCalories,
            int proteinGram,
            int fatGram
    ) {
        int proteinCalories = proteinGram * 4;
        int fatCalories = fatGram * 9;

        return (targetCalories - proteinCalories - fatCalories) / 4;
    }
}