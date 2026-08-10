package com.fitplate.fitplateapi.mealplan.controller;

import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.mealplan.dto.*;
import com.fitplate.fitplateapi.mealplan.service.MealPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/meal-plan")
@Tag(
    name = "식단 관리",
    description = "AI 식단 생성, 저장, 조회, 삭제 기능"
)
@RequiredArgsConstructor
@Slf4j
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 식단 생성 (POST /api/meal-plan)
     */
    @Operation(
        summary = "AI 식단 생성",
        description = """
            사용자의 신체 정보와 목표를 기반으로 하루 식단을 생성합니다.

            사전 조건:
            - 사용자 프로필이 등록되어 있어야 합니다.
            - 유효한 JWT가 필요합니다.
            """
    )
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
     * 사용자의 전체 식단 조회 (GET /api/meal-plan)
     */
    @Operation(
        summary = "사용자의 전체 식단 조회",
        description = """
            JWT에 포함된 사용자 키를 기준으로 사용자가 저장한 전체 식단을 조회합니다.

            식단은 최신 생성순으로 반환되며, 각 항목에는 식단 ID, 신체 정보, 영양 목표,
            AI가 생성한 식단 내용과 생성·수정 시각이 포함됩니다.
            저장된 식단이 없으면 빈 배열을 반환하며, 유효한 JWT가 필요합니다.
            """
    )
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
    @Operation(
        summary = "특정 식단 상세 조회",
        description = """
            식단 ID로 저장된 특정 식단의 상세 정보를 조회합니다.

            신체 정보, 목표 칼로리, 기초대사량(BMR), 활동대사량(TDEE),
            탄수화물·단백질·지방 목표량과 AI가 생성한 식단 전체 내용을 반환합니다.
            해당 ID의 식단이 없으면 404 Not Found를 반환합니다.
            """
    )
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
    @Operation(
        summary = "특정 식단 삭제",
        description = """
            식단 ID에 해당하는 로그인 사용자의 식단을 삭제합니다.

            해당 ID의 식단이 없으면 404 Not Found를 반환하며,
            다른 사용자가 소유한 식단은 삭제할 수 없습니다.
            삭제 성공 시 204 No Content를 반환하며, 유효한 JWT가 필요합니다.
            """
    )
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
