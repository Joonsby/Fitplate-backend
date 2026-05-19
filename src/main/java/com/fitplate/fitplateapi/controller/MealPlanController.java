package com.fitplate.fitplateapi.controller;

import com.fitplate.fitplateapi.dto.MealPlanRequest;
import com.fitplate.fitplateapi.dto.MealPlanResponse;
import com.fitplate.fitplateapi.service.MealPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 식단 관련 API의 엔드포인트를 담당하는 컨트롤러
 */
@RestController
@RequestMapping("/api/meal-plan")
public class MealPlanController {

    // 식단 생성 서비스 주입
    private final MealPlanService mealPlanService;

    /**
     * 생성자를 통한 의존성 주입
     *
     * @param mealPlanService 식단 생성 서비스
     */
    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    /**
     * 사용자 정보와 목표를 받아 맞춤형 식단을 생성하는 API
     *
     * @param request 사용자의 신체 정보 및 목표 (현재는 Mock 응답 반환)
     * @return 생성된 식단 계획
     */
    @PostMapping
    public ResponseEntity<MealPlanResponse> generateMealPlan(@Valid @RequestBody MealPlanRequest request) {
        // 서비스에서 식단 생성
        MealPlanResponse response = mealPlanService.generateMealPlan(request);

        // 200 OK 상태코드와 함께 결과 반환
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

