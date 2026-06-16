package com.fitplate.fitplateapi.nutrition.service;

import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class NutritionCalculator {
    public NutritionResult calculate(
            Integer height,
            Integer weight,
            Integer age,
            Double bodyFatRate,
            String gender,
            String goal
    ) {
        // 신체 정보와 목표로 BMR/TDEE/목표칼로리/영양소(g)를 계산해 반환.
        int bmr = calculateBmr(height, weight, age, bodyFatRate, gender);
        int tdee = calculateTdee(bmr);
        int targetCalories = calculateTargetCalories(tdee, goal);

        int proteinGram = calculateProteinGram(weight);
        int fatGram = calculateFatGram(targetCalories);
        int carbsGram = calculateCarbsGram(targetCalories, proteinGram, fatGram);

        return NutritionResult.builder()
                .bmr(bmr)
                .tdee(tdee)
                .targetCalories(targetCalories)
                .proteinGram(proteinGram)
                .fatGram(fatGram)
                .carbsGram(carbsGram)
                .build();
    }

    private int calculateBmr(Integer height, Integer weight, Integer age, Double bodyFatRate, String gender) {
        // 체지방률 있으면 Katch-McArdle, 없으면 성별별 Mifflin-St Jeor 공식.
        if (bodyFatRate != null) {
            double leanBodyMass = weight * (1 - (bodyFatRate / 100.0));
            return (int) Math.round(370 + (21.6 * leanBodyMass));
        }

        if ("MALE".equalsIgnoreCase(gender)) {
            return (int) Math.round(10 * weight + 6.25 * height - 5 * age + 5);
        }

        return (int) Math.round(10 * weight + 6.25 * height - 5 * age - 161);
    }

    private int calculateTdee(int bmr) {
        // TDEE = BMR * 활동계수(1.35).
        return (int) Math.round(bmr * 1.35);
    }

    private int calculateTargetCalories(int tdee, String goal) {
        // goal별 감량/증량 칼로리 조정.
        return switch (goal) {
            case "WEIGHT_LOSS" -> tdee - 400;
            case "MUSCLE_GAIN" -> tdee + 300;
            default -> tdee;
        };
    }

    private int calculateProteinGram(Integer weight) {
        // 단백질(g) = 체중 * 1.6g/kg.
        return (int) Math.round(weight * 1.6);
    }

    private int calculateFatGram(int targetCalories) {
        // 지방(g): 칼로리의 25%를 9kcal/g로 환산.
        return (int) Math.round((targetCalories * 0.25) / 9);
    }

    private int calculateCarbsGram(int targetCalories, int proteinGram, int fatGram) {
        // 탄수화물(g): 단백질/지방 칼로리 제외 잔여를 4kcal/g로 환산.
        return (targetCalories - proteinGram * 4 - fatGram * 9) / 4;
    }

    public BigDecimal calculateBmi(Integer height, Integer weight) {
        // BMI = 체중 / 키(m)^2, 소수점 둘째자리 반올림.
        double heightM = height / 100.0;
        double bmi = weight / (heightM * heightM);
        return BigDecimal.valueOf(bmi).setScale(2, RoundingMode.HALF_UP);
    }
}
