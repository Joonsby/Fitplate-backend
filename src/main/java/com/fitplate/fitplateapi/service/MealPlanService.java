package com.fitplate.fitplateapi.service;

import com.fitplate.fitplateapi.dto.MealDayResponse;
import com.fitplate.fitplateapi.dto.MealPlanRequest;
import com.fitplate.fitplateapi.dto.MealPlanResponse;
import com.fitplate.fitplateapi.dto.MealResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 식단 생성 비즈니스 로직을 담당하는 서비스
 * 현재는 Mock 데이터를 반환합니다
 */
@Service
public class MealPlanService {

    /**
     * 사용자의 신체 정보와 목표를 바탕으로 맞춤형 식단을 생성합니다
     *
     * @param request 사용자의 신체 정보 및 목표 (height, weight, age, gender, goal, periodDays 등)
     * @return 지정된 기간의 식단 계획
     */
    public MealPlanResponse generateMealPlan(MealPlanRequest request) {
        // Mock 데이터로 식단 계획 생성
        List<MealDayResponse> days = new ArrayList<>();

        // 요청한 기간만큼 일일 식단 생성
        for (int dayNumber = 1; dayNumber <= request.getPeriodDays(); dayNumber++) {
            days.add(createMockMealDay(dayNumber));
        }

        // 생성된 식단 반환
        return MealPlanResponse.builder()
            .days(days)
            .build();
    }

    /**
     * Mock 데이터를 사용하여 하루의 식단을 생성합니다
     *
     * @param dayNumber 날짜 번호
     * @return 하루의 식단 정보
     */
    private MealDayResponse createMockMealDay(int dayNumber) {
        return MealDayResponse.builder()
            .dayNumber(dayNumber)
            .breakfast(createMockBreakfast())
            .lunch(createMockLunch())
            .dinner(createMockDinner())
            .build();
    }

    /**
     * Mock 아침 식사 데이터를 생성합니다
     */
    private MealResponse createMockBreakfast() {
        return MealResponse.builder()
            .name("계란말이, 흰쌀밥, 미역국")
            .calories(650)
            .protein(28.5)
            .carbohydrate(72.0)
            .fat(18.2)
            .build();
    }

    /**
     * Mock 점심 식사 데이터를 생성합니다
     */
    private MealResponse createMockLunch() {
        return MealResponse.builder()
            .name("불고기덮밥, 계란찜, 깍두기")
            .calories(750)
            .protein(35.2)
            .carbohydrate(85.0)
            .fat(20.5)
            .build();
    }

    /**
     * Mock 저녁 식사 데이터를 생성합니다
     */
    private MealResponse createMockDinner() {
        return MealResponse.builder()
            .name("연어구이, 현미밥, 시금치나물")
            .calories(680)
            .protein(42.0)
            .carbohydrate(68.0)
            .fat(16.8)
            .build();
    }
}

