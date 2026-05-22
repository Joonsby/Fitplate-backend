package com.fitplate.fitplateapi.mealplan.controller;

import com.fitplate.fitplateapi.mealplan.dto.*;
import com.fitplate.fitplateapi.mealplan.service.MealPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController  // @Controller + @ResponseBody 합친 것. JSON으로 응답함
@RequestMapping("/api/meal-plan")  // 이 컨트롤러의 기본 URL 경로
public class MealPlanController {

    private final MealPlanService mealPlanService;

    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    @PostMapping  // POST /api/meal-plan
    public ResponseEntity<MealPlanResponse> generateMealPlan(@Valid @RequestBody MealPlanRequest request) {
        // Step 1: Service에서 식단 생성 (비즈니스 로직 실행)
        MealPlanResponse response = mealPlanService.generateMealPlan(request);

        // Step 2: 200 OK 상태코드와 함께 결과 반환
        // ResponseEntity: HTTP 상태 코드 + 응답 본문을 함께 반환
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

   /*
   * 저장된 식단 목록 조회 (GET /api/meal-plans)
   * */
    @GetMapping("/{tossUserKey}")
    public ResponseEntity<List<SavedMealPlanResponse>> getSavedMealPlans(@PathVariable String tossUserKey) {
        List<SavedMealPlanResponse> response = mealPlanService.getSavedMealPlans(tossUserKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{mealPlanId}")
    public ResponseEntity<MealPlanDetailResponse> getMealPlan(
            @PathVariable Long mealPlanId
    ) {
        return ResponseEntity.ok(mealPlanService.findById(mealPlanId));
    }
}



