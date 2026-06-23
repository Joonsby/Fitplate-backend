package com.fitplate.fitplateapi.mealplan.controller;

import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.mealplan.dto.*;
import com.fitplate.fitplateapi.mealplan.service.MealPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/meal-plan")
@RequiredArgsConstructor
@Slf4j
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 식단 생성 (POST /api/meal-plan)
     */
    @PostMapping
    public ResponseEntity<MealPlanGenerateResponse> generateMealPlan(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody MealPlanRequest request
    ) {
        String tossUserKey = extractTossUserKey(authorization);
        MealPlanGenerateResponse response = mealPlanService.generateMealPlan(tossUserKey,request);
        return ResponseEntity.ok(response);
    }

   /**
    * 저장된 식단 목록 조회 (GET /api/meal-plan/users/{tossUserKey})
    */
    @GetMapping
    public ResponseEntity<List<SavedMealPlanResponse>> getSavedMealPlans(
            @RequestHeader("Authorization") String authorization
    ) {
        String tossUserKey = extractTossUserKey(authorization);
        List<SavedMealPlanResponse> response = mealPlanService.getSavedMealPlans(tossUserKey);
        return ResponseEntity.ok(response);
    }

    /**
     * 식단 상세 조회 (GET /api/meal-plan/{id})
     */
    @GetMapping("/{id}")
    public ResponseEntity<MealPlanDetailResponse> getMealPlan(@PathVariable Long id) {
        return ResponseEntity.ok(mealPlanService.findById(id));
    }

    private String extractTossUserKey(String authorization) {
        String token = jwtTokenProvider.resolveToken(authorization);
        return jwtTokenProvider.getTossUserKey(token);
    }

    /**
     * 식단 삭제 (DELETE /api/meal-plan/{id})
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMealPlan(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id
    ) {
        String tossUserKey = extractTossUserKey(authorization);
        mealPlanService.deleteMealPlan(tossUserKey, id);
        return ResponseEntity.noContent().build();
    }

}