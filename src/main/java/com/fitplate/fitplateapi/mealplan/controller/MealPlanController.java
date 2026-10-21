package com.fitplate.fitplateapi.mealplan.controller;

import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.mealplan.dto.*;
import com.fitplate.fitplateapi.mealplan.service.MealIntakeService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanEditService;
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
    description = "AI 식단 생성, 저장, 조회, 삭제, 음식 바꾸기 기능"
)
@RequiredArgsConstructor
@Slf4j
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final MealPlanEditService mealPlanEditService;
    private final MealIntakeService mealIntakeService;
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


    /**
     * 음식 대안 조회 (POST /api/meal-plan/{id}/foods/alternatives)
     */
    @Operation(
        summary = "음식 대안 조회",
        description = """
            식단의 음식 한 개를 대신할 대안 음식을 최대 3개 돌려줍니다.

            즐겨찾기, 저장된 대안, AI 순으로 채웁니다. excludeNames 에 넣은 음식은 다시 제안하지 않습니다.
            대안은 기존 음식과 칼로리가 비슷하고(±15%), 같은 끼니의 다른 음식과 겹치지 않으며,
            사용자가 제외한 음식 분류에 해당하지 않습니다.
            조건에 맞는 대안을 찾지 못하면 503, 오늘 AI 호출 상한을 넘으면 429, 다른 사용자의 식단이면 403을 반환합니다.
            """
    )
    @PostMapping("/{id}/foods/alternatives")
    public ResponseEntity<FoodAlternativesResponse> suggestFoodAlternatives(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @Valid @RequestBody FoodPositionRequest request
    ) {
        String tossUserKey = extractTossUserKey(authorization);
        return ResponseEntity.ok(mealPlanEditService.suggestAlternatives(
                tossUserKey, id, request.getMealType(), request.getFoodIndex(), request.getExcludeNames()));
    }

    /**
     * 음식 바꾸기 (PUT /api/meal-plan/{id}/foods)
     */
    @Operation(
        summary = "음식 바꾸기",
        description = """
            식단의 음식 한 개를 다른 음식으로 바꿔 저장하고, 바뀐 식단 전체를 반환합니다.

            식단 균형을 지키기 위해 기존 음식과 칼로리 차이가 ±20%를 넘으면 400을 반환합니다.
            다른 사용자의 식단이면 403을 반환합니다.
            """
    )
    @PutMapping("/{id}/foods")
    public ResponseEntity<SavedMealPlanResponse> replaceFood(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @Valid @RequestBody FoodReplaceRequest request
    ) {
        String tossUserKey = extractTossUserKey(authorization);
        return ResponseEntity.ok(mealPlanEditService.replaceFood(
                tossUserKey, id, request.getMealType(), request.getFoodIndex(), request.getFood()));
    }

    /**
     * 끼니 재생성 (POST /api/meal-plan/{id}/meals/{mealType}/regenerate)
     */
    @Operation(
        summary = "끼니 재생성",
        description = """
            식단의 한 끼(breakfast, lunch, dinner)를 AI 가 새 음식 조합으로 다시 구성해 저장하고,
            바뀐 식단 전체를 반환합니다. 새 끼니는 기존 끼니의 칼로리·탄단지 합계에 맞추고
            제외한 음식 분류를 피합니다. 조건에 맞는 끼니를 만들지 못하면 503,
            오늘 재생성 상한을 넘으면 429를 반환합니다.
            """
    )
    @PostMapping("/{id}/meals/{mealType}/regenerate")
    public ResponseEntity<SavedMealPlanResponse> regenerateMeal(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @PathVariable String mealType
    ) {
        String tossUserKey = extractTossUserKey(authorization);
        return ResponseEntity.ok(mealPlanEditService.regenerateMeal(tossUserKey, id, mealType));
    }

    /**
     * 식단 이름 변경 (PATCH /api/meal-plan/{id}/title)
     */
    @Operation(
        summary = "식단 이름 변경",
        description = """
            식단 이름을 30자 이내로 바꿉니다. title 을 비우거나 null 로 보내면 자동 이름으로 돌아갑니다.
            다른 사용자의 식단이면 403을 반환합니다.
            """
    )
    @PatchMapping("/{id}/title")
    public ResponseEntity<SavedMealPlanResponse> renameMealPlan(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @Valid @RequestBody MealPlanTitleRequest request
    ) {
        String tossUserKey = extractTossUserKey(authorization);
        return ResponseEntity.ok(mealPlanEditService.renamePlan(tossUserKey, id, request.getTitle()));
    }

    /**
     * 오늘 식단 생성 가능 횟수 (GET /api/meal-plan/generation-quota)
     */
    @Operation(
        summary = "오늘 식단 생성 가능 횟수",
        description = """
            오늘 만든 횟수, 하루 상한, 남은 횟수와 다음 생성에 광고가 필요한지(adRequired)를 돌려줍니다.
            하루 첫 생성은 광고 없이 만들고, 상한을 넘으면 생성 API 가 429를 반환합니다.
            """
    )
    @GetMapping("/generation-quota")
    public ResponseEntity<GenerationQuotaResponse> getGenerationQuota(
            @RequestHeader("Authorization") String authorization
    ) {
        return ResponseEntity.ok(mealPlanService.getGenerationQuota(extractTossUserKey(authorization)));
    }

    /**
     * 식단을 다른 날짜로 복사 (POST /api/meal-plan/{id}/copy)
     */
    @Operation(
        summary = "식단을 다른 날짜로 복사",
        description = """
            같은 음식으로 새 식단을 만들어 planDate 에 지정하고, 새 식단을 반환합니다. 원래 식단은 바뀌지 않습니다.
            그날 다른 식단이 있으면 409(DATE_TAKEN)이고, replace=true 로 다시 보내면 그 식단의 날짜 지정을 풀고 복사합니다.
            """
    )
    @PostMapping("/{id}/copy")
    public ResponseEntity<SavedMealPlanResponse> copyMealPlan(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @RequestBody MealPlanScheduleRequest request
    ) {
        String tossUserKey = extractTossUserKey(authorization);
        return ResponseEntity.ok(mealIntakeService.copyToDate(tossUserKey, id, request.getPlanDate(), request.isReplace()));
    }

    /**
     * 식단 날짜 지정 (PUT /api/meal-plan/{id}/schedule)
     */
    @Operation(
        summary = "식단 날짜 지정",
        description = """
            이 식단을 따를 날짜를 정합니다. 날짜마다 식단은 하나이고, planDate 가 null 이면 지정을 해제합니다.
            같은 날짜에 다른 식단이 있으면 409(code=DATE_TAKEN)이고, replace=true 로 보내면 바꿉니다.
            먹었다고 체크한 끼니가 있는 식단은 날짜를 바꾸거나 해제할 수 없습니다(409, code=HAS_INTAKE).
            """
    )
    @PutMapping("/{id}/schedule")
    public ResponseEntity<SavedMealPlanResponse> scheduleMealPlan(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @RequestBody MealPlanScheduleRequest request
    ) {
        String tossUserKey = extractTossUserKey(authorization);
        return ResponseEntity.ok(mealIntakeService.schedule(tossUserKey, id, request.getPlanDate(), request.isReplace()));
    }

    /**
     * 끼니 섭취 기록 (PUT /api/meal-plan/{id}/meals/{mealType}/intake)
     */
    @Operation(
        summary = "끼니 섭취 기록",
        description = """
            끼니를 먹었는지 기록합니다. 원하는 상태(eaten)를 보내므로 같은 요청을 반복해도 결과가 같습니다.
            날짜가 지정되지 않은 식단은 409(code=NOT_SCHEDULED), 아직 오지 않은 날짜에 먹었다고 하면
            409(code=FUTURE_DATE)입니다. 먹은 끼니는 음식 바꾸기·다시 만들기가 막힙니다(code=MEAL_EATEN).
            """
    )
    @PutMapping("/{id}/meals/{mealType}/intake")
    public ResponseEntity<SavedMealPlanResponse> setMealIntake(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @PathVariable String mealType,
            @Valid @RequestBody MealIntakeRequest request
    ) {
        String tossUserKey = extractTossUserKey(authorization);
        return ResponseEntity.ok(mealIntakeService.setIntake(tossUserKey, id, mealType, request.getEaten()));
    }
}
