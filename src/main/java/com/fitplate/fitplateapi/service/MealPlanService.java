package com.fitplate.fitplateapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.client.GeminiMealPlanClient;
import com.fitplate.fitplateapi.domain.MealPlan;
import com.fitplate.fitplateapi.domain.User;
import com.fitplate.fitplateapi.dto.MealPlanRequest;
import com.fitplate.fitplateapi.dto.MealPlanResponse;
import com.fitplate.fitplateapi.repository.MealPlanRepository;
import com.fitplate.fitplateapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
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

    @Transactional
    public MealPlanResponse generateMealPlan(MealPlanRequest request) {
        int bmr = calculateBmr(request);
        int tdee = calculateTdee(bmr);
        int targetCalories = calculateTargetCalories(
                tdee,
                request.getGoal()
        );
        int proteinGram = calculateProteinGram(request);
        int fatGram = calculateFatGram(targetCalories);
        int carbsGram = calculateCarbsGram(
                targetCalories,
                proteinGram,
                fatGram
        );

        MealPlanResponse response = geminiMealPlanClient.generateMealPlan(request);

        User user = userRepository.findByTossUserKey(MOCK_USER_KEY)
                .orElseGet(() -> userRepository.save(new User(MOCK_USER_KEY, "test_user")));

        String aiResponseJson = toJson(response);

        LocalDateTime now = LocalDateTime.now();

        MealPlan mealPlan = new MealPlan(
                user,
                request.getGoal(),
                request.getPeriodDays(),
                request.getHeight(),
                request.getWeight(),
                request.getAge(),
                request.getGender(),
                targetCalories,
                bmr,
                tdee,
                proteinGram,
                carbsGram,
                fatGram,
                aiResponseJson,
                now,
                now.plusDays(request.getPeriodDays())
        );

        mealPlanRepository.save(mealPlan);

        return response;
    }

    private String toJson(MealPlanResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 식단 응답 JSON 변환에 실패했습니다.", e);
        }
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