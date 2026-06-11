package com.fitplate.fitplateapi.mealplan.controller;

import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.mealplan.dto.*;
import com.fitplate.fitplateapi.mealplan.service.MealPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController  // @Controller + @ResponseBody 합친 것. JSON으로 응답함
@RequestMapping("/api/meal-plan")  // 이 컨트롤러의 기본 URL 경로
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성 (의존성 주입)
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
     * 식단 저장 (POST /api/meal-plan/save)
     */
    @PostMapping("/save")
    public ResponseEntity<Long> saveMealPlan(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody SaveMealPlanRequest request
    ) {
        String tossUserKey = extractTossUserKey(authorization);
        Long mealPlanId = mealPlanService.saveMealPlan(tossUserKey,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mealPlanId);
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
     * 식단 상세 조회 (GET /api/meal-plan/detail/{mealPlanId})
     */
    @GetMapping("/detail/{mealPlanId}")
    public ResponseEntity<MealPlanDetailResponse> getMealPlan(@PathVariable Long mealPlanId) {
        return ResponseEntity.ok(mealPlanService.findById(mealPlanId));
    }

    private String extractTossUserKey(String authorization) {
        String token = jwtTokenProvider.resolveToken(authorization);
        return jwtTokenProvider.getTossUserKey(token);
    }

    /**
     * 식단 삭제 (DELETE /api/meal-plan/{mealPlanId})
     */
    @DeleteMapping("/{mealPlanId}")
    public ResponseEntity<Void> deleteMealPlan(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long mealPlanId
    ) {
        String tossUserKey = extractTossUserKey(authorization);
        mealPlanService.deleteMealPlan(tossUserKey, mealPlanId);
        return ResponseEntity.noContent().build();
    }

}



