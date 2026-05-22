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

    /**
     * 식단 생성 (POST /api/meal-plan)
     */
    @PostMapping
    public ResponseEntity<MealPlanResponse> generateMealPlan(@Valid @RequestBody MealPlanRequest request) {
        MealPlanResponse response = mealPlanService.generateMealPlan(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

//    @PostMapping("/save")
//    public ResponseEntity<Long> saveMealPlan(@Valid @RequestBody SaveMealPlanRequest request) {
//        mealPlanService.saveMealPlan(request);
//        return ResponseEntity.status(HttpStatus.OK).build();
//    }

   /**
    * 저장된 식단 목록 조회 (GET /api/meal-plans)
    */
    @GetMapping("/{tossUserKey}")
    public ResponseEntity<List<SavedMealPlanResponse>> getSavedMealPlans(@PathVariable String tossUserKey) {
        List<SavedMealPlanResponse> response = mealPlanService.getSavedMealPlans(tossUserKey);
        return ResponseEntity.ok(response);
    }

    /**
     * 식단 상세 조회 (GET /api/meal-plan/{mealPlanId})
     */
    @GetMapping("/{mealPlanId}")
    public ResponseEntity<MealPlanDetailResponse> getMealPlan(
            @PathVariable Long mealPlanId
    ) {
        return ResponseEntity.ok(mealPlanService.findById(mealPlanId));
    }
}



