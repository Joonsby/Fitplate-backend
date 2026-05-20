package com.fitplate.fitplateapi.mealplan.controller;

import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import com.fitplate.fitplateapi.mealplan.service.MealPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 식단 관련 API의 엔드포인트를 담당하는 컨트롤러
 *
 * 📌 컨트롤러의 역할:
 * - HTTP 요청을 받아서 어떤 작업을 할지 결정합니다
 * - 요청 데이터 검증과 유효성 확인
 * - 서비스 계층에 비즈니스 로직을 위임
 * - HTTP 응답을 반환
 *
 * 📌 API 구조:
 * ┌─────────────────────────────────────────────────────────┐
 * │ 클라이언트(프론트엔드)                                      │
 * └──────────────────┬──────────────────────────────────────┘
 *                    │ HTTP 요청
 *                    ▼
 * ┌─────────────────────────────────────────────────────────┐
 * │ Controller (MealPlanController)                          │
 * │ - 요청 받기 (POST /api/meal-plan)                       │
 * │ - 입력 데이터 검증 (@Valid)                             │
 * └──────────────────┬──────────────────────────────────────┘
 *                    │ 비즈니스 로직 처리
 *                    ▼
 * ┌─────────────────────────────────────────────────────────┐
 * │ Service (MealPlanService)                               │
 * │ - 실제 식단 생성 로직 실행                               │
 * │ - AI API 호출                                           │
 * │ - DB에 데이터 저장                                       │
 * └──────────────────┬──────────────────────────────────────┘
 *                    │ JSON 응답
 *                    ▼
 * ┌─────────────────────────────────────────────────────────┐
 * │ 클라이언트(프론트엔드)                                      │
 * └─────────────────────────────────────────────────────────┘
 */
@RestController  // @Controller + @ResponseBody 합친 것. JSON으로 응답함
@RequestMapping("/api/meal-plan")  // 이 컨트롤러의 기본 URL 경로
public class MealPlanController {

    // 의존성 주입(Dependency Injection)
    // Service 계층에서 비즈니스 로직을 처리하므로 Service 객체가 필요합니다
    // final이므로 초기화 후 값 변경 불가능 (안전성 보장)
    private final MealPlanService mealPlanService;

    /**
     * 생성자를 통한 의존성 주입 (Constructor Injection)
     *
     * 📌 왜 생성자 주입을 사용하나?
     * - Setter 주입보다 안전하고 확실함
     * - final로 선언 가능 (불변성 보장)
     * - 순환 의존성 문제  방지
     * - 테스트하기 더 쉬움
     *
     * @param mealPlanService 스프링이 자동으로 생성하여 주입해주는 서비스 객체
     */
    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    /**
     * 사용자 정보와 목표를 받아 맞춤형 식단을 생성하는 API
     *
     * 📌 API 요청 흐름:
     * 1. 클라이언트가 POST 요청 + JSON 데이터 전송
     * 2. @Valid 검증 수행
     *    - 키: 100~300cm
     *    - 체중: 20~300kg
     *    - 나이: 10~120세
     *    - 목표: WEIGHT_LOSS, MUSCLE_GAIN, DIET_BALANCE 등
     * 3. MealPlanService.generateMealPlan() 호출
     * 4. 생성된 식단 데이터 반환
     *
     * 📌 요청 예시:
     * POST /api/meal-plan
     * Content-Type: application/json
     *
     * {
     *   "height": 175,          // 키(cm)
     *   "weight": 70,           // 체중(kg)
     *   "age": 30,              // 나이
     *   "gender": "MALE",       // 성별
     *   "bodyFatRate": 20.5,    // 체지방률(선택사항)
     *   "goal": "MUSCLE_GAIN",  // 목표
     *   "periodDays": 7         // 식단 기간(일)
     * }
     *
     * 📌 응답 예시:
     * 200 OK
     * {
     *   "days": [
     *     {
     *       "dayNumber": 1,
     *       "breakfast": {...},
     *       "lunch": {...},
     *       "dinner": {...}
     *     },
     *     ...
     *   ]
     * }
     *
     * @param request @Valid 검증된 사용자 요청 데이터
     * @return ResponseEntity: HTTP 상태 코드 + 식단 데이터
     */
    @PostMapping  // POST /api/meal-plan
    public ResponseEntity<MealPlanResponse> generateMealPlan(@Valid @RequestBody MealPlanRequest request) {
        // Step 1: Service에서 식단 생성 (비즈니스 로직 실행)
        MealPlanResponse response = mealPlanService.generateMealPlan(request);

        // Step 2: 200 OK 상태코드와 함께 결과 반환
        // ResponseEntity: HTTP 상태 코드 + 응답 본문을 함께 반환
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}



