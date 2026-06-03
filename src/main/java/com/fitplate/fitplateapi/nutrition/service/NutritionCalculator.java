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
        // 전체 영양소 계산을 수행합니다.
        // 주어진 신체 정보와 목표에 따라 BMR, TDEE, 목표 칼로리 및 영양소(g)를 계산해
        // NutritionResult 객체로 반환합니다.
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
        // BMR(기초대사량)을 계산합니다.
        // - 체지방률(bodyFatRate)이 제공되면 Lean Body Mass 기반 Katch-McArdle 공식을 사용합니다.
        // - 체지방률이 없으면 성별에 따른 Mifflin-St Jeor 공식을 사용합니다.
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
        // TDEE(총 일일 에너지 소비량)를 추정합니다.
        // 여기서는 활동계수 1.35를 사용하여 BMR을 확장합니다.
        return (int) Math.round(bmr * 1.35);
    }

    private int calculateTargetCalories(int tdee, String goal) {
        // 목표 칼로리를 계산합니다.
        // goal에 따라 감량/증량을 위한 고정 칼로리 조정값을 적용합니다.
        return switch (goal) {
            case "WEIGHT_LOSS" -> tdee - 400;
            case "MUSCLE_GAIN" -> tdee + 300;
            default -> tdee;
        };
    }

    private int calculateProteinGram(Integer weight) {
        // 단백질(g)을 계산합니다.
        // 체중(kg) 당 권장 단백질 섭취량(여기서는 1.6g/kg)을 사용합니다.
        return (int) Math.round(weight * 1.6);
    }

    private int calculateFatGram(int targetCalories) {
        // 지방(g)을 계산합니다.
        // 전체 칼로리 중 지방 비율(여기서는 25%)을 기준으로, 지방 1g당 9kcal로 환산합니다.
        return (int) Math.round((targetCalories * 0.25) / 9);
    }

    private int calculateCarbsGram(int targetCalories, int proteinGram, int fatGram) {
        // 탄수화물(g)을 계산합니다.
        // 목표 칼로리에서 단백질과 지방의 칼로리를 제외한 나머지를
        // 탄수화물(4kcal/g)으로 환산하여 그람 수를 구합니다.
        return (targetCalories - proteinGram * 4 - fatGram * 9) / 4;
    }

    public BigDecimal calculateBmi(Integer height, Integer weight) {
        // BMI(체질량지수)를 계산합니다.
        // 키(cm)를 m로 변환하여 체중(kg)을 키(m)^2로 나눈 후
        // 소수점 둘째자리(HALF_UP)로 반올림하여 BigDecimal로 반환합니다.
        double heightM = height / 100.0;
        double bmi = weight / (heightM * heightM);
        return BigDecimal.valueOf(bmi).setScale(2, RoundingMode.HALF_UP);
    }
}
