package com.fitplate.fitplateapi.mealplan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.ai.GeminiMealPlanClient;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.mealplan.dto.SavedMealPlanResponse;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import com.fitplate.fitplateapi.mealplan.repository.MealPlanRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 식단 생성 비즈니스 로직을 담당하는 Service 클래스
 *
 * 📌 Service 계층의 역할:
 * - 비즈니스 로직 실행 (식단 생성, 데이터 계산 등)
 * - Repository를 통해 DB와 상호작용
 * - 외부 API 호출 (Gemini AI)
 * - 트랜잭션 관리
 *
 * 📌 generateMealPlan() 실행 흐름:
 * 1. 사용자 신체 정보로부터 칼로리 계산
 *    - BMR(기초대사량) 계산
 *    - TDEE(일일 총 소비 칼로리) 계산
 *    - 목표에 따른 목표 칼로리 조정
 * 2. 영양소 분배 계산
 *    - 단백질, 탄수화물, 지방 그램 계산
 * 3. AI 모델(Gemini)에 요청하여 식단 생성
 * 4. 사용자 정보 저장 (없으면 생성)
 * 5. 생성된 식단을 MealPlan 엔티티로 저장
 * 6. 클라이언트에 응답
 */
@Service  // @Component의 특수한 형태. 비즈니스 로직 담당
public class MealPlanService {

    // Mock 사용자 키 (프로토타입 개발 중이므로 고정값 사용)
    private static final String MOCK_USER_KEY = "MOCK_USER_001";

    // 의존성 주입: Gemini AI 클라이언트
    private final GeminiMealPlanClient geminiMealPlanClient;

    // 의존성 주입: User 데이터 접근 객체
    private final UserRepository userRepository;

    // 의존성 주입: MealPlan 데이터 접근 객체
    private final MealPlanRepository mealPlanRepository;

    // 의존성 주입: JSON 변환 유틸리티
    private final ObjectMapper objectMapper;

    /**
     * 생성자 의존성 주입
     * Spring이 자동으로 필요한 객체들을 주입해줍니다
     */
    public MealPlanService(
            GeminiMealPlanClient geminiMealPlanClient,
            UserRepository userRepository,
            MealPlanRepository mealPlanRepository,
            ObjectMapper objectMapper
    ) {
        this.geminiMealPlanClient = geminiMealPlanClient;
        this.userRepository = userRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 사용자 정보로부터 맞춤형 식단을 생성합니다
     *
     * @Transactional: 이 메서드의 모든 DB 작업이 하나의 트랜잭션으로 처리됩니다
     *                 도중에 예외가 발생하면 모든 작업이 롤백됩니다 (All or Nothing)
     *
     * @param request 사용자의 신체 정보 및 목표
     * @return AI가 생성한 식단 계획
     */
    @Transactional
    public MealPlanResponse generateMealPlan(MealPlanRequest request) {
        // ========== Step 1: 칼로리 관련 계산 ==========

        // BMR(Basal Metabolic Rate, 기초대사량):
        // 아무것도 하지 않았을 때 몸이 소비하는 칼로리
        // 남성과 여성의 공식이 다름
        int bmr = calculateBmr(request);

        // TDEE(Total Daily Energy Expenditure, 일일 총 소비 칼로리):
        // BMR * 활동 수준 계수 (기본값 1.35 = 중등도 활동)
        int tdee = calculateTdee(bmr);

        // 목표에 따라 칼로리 조정
        // 체중감량: TDEE - 400 (칼로리 부족)
        // 근육증가: TDEE + 300 (칼로리 과잉)
        // 기타: TDEE (유지)
        int targetCalories = calculateTargetCalories(tdee, request.getGoal());

        // ========== Step 2: 영양소 분배 계산 ==========

        // 단백질 그램 : 체중 당 1.6g
        // (근육 유지/성장에 중요)
        int proteinGram = calculateProteinGram(request);

        // 지방 그램 : 목표 칼로리의 25% (= 칼로리 / 9)
        // (9 = 지방 1g당 칼로리)
        int fatGram = calculateFatGram(targetCalories);

        // 탄수화물 그램 : 나머지 칼로리로 계산
        // (4 = 탄수화물 1g당 칼로리)
        int carbsGram = calculateCarbsGram(targetCalories, proteinGram, fatGram);

        // ========== Step 3: AI 모델에서 식단 생성 ==========

        // Gemini AI API에 요청을 보내고 맞춤형 식단을 받습니다
        // (한국식 식단으로 아침, 점심, 저녁 포함)
        MealPlanResponse response = geminiMealPlanClient.generateMealPlan(request);

        // ========== Step 4: 사용자 정보 조회 또는 생성 ==========

        // 데이터베이스에서 사용자 조회
        // Optional = 값이 있을 수도, 없을 수도 있는 상황을 안전하게 처리
        User user = userRepository.findByTossUserKey(MOCK_USER_KEY)
                // 사용자가 있으면 반환
                .orElseGet(() ->
                    // 없으면 새로운 사용자 생성 후 저장
                    userRepository.save(new User(MOCK_USER_KEY, "test_user"))
                );

        // ========== Step 5: AI 응답을 JSON 문자열로 변환 ==========

        // MealPlanResponse 객체를 JSON 문자열로 변환하여 DB에 저장
        // (나중에 식단의 상세 데이터를 다시 필요할 때 사용)
        String aiResponseJson = toJson(response);

        // ========== Step 6: MealPlan 엔티티 생성 ==========

        // 현재 시간
        LocalDateTime now = LocalDateTime.now();

        // 식단 정보를 DB에 저장할 MealPlan 엔티티 생성
        MealPlan mealPlan = new MealPlan(
                user,                          // 어느 사용자의 식단인가
                request.getGoal(),              // 목표 (WEIGHT_LOSS 등)
                request.getPeriodDays(),        // 기간 (7일, 30일 등)
                request.getHeight(),            // 키
                request.getWeight(),            // 체중
                request.getAge(),               // 나이
                request.getGender(),            // 성별
                targetCalories,                 // 목표 칼로리
                bmr,                            // 기초대사량
                tdee,                           // 일일 총 소비 칼로리
                proteinGram,                    // 단백질
                carbsGram,                      // 탄수화물
                fatGram,                        // 지방
                aiResponseJson,                 // AI 응답 (JSON)
                now,                            // 시작 시간
                now.plusDays(request.getPeriodDays())  // 종료 시간
        );

        // ========== Step 7: 데이터베이스에 저장 ==========

        // MealPlan을 DB의 meal_plans 테이블에 저장
        // @Transactional 덕분에 dao 작업도 같은 트랜잭션에 포함됨
        mealPlanRepository.save(mealPlan);

        // ========== Step 8: 클라이언트에 응답 반환 ==========

        // AI가 생성한 식단 정보를 클라이언트에게 반환
        return response;
    }

    /**
     * JSON 변환 헬퍼 메서드
     * Object 타입을 JSON 문자열로 변환
     */
    private String toJson(MealPlanResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            // JSON 변환 실패시 예외 발생
            throw new IllegalStateException("AI 식단 응답 JSON 변환에 실패했습니다.", e);
        }
    }

    /**
     * BMR(기초대사량) 계산
     *
     * 📌 Mifflin-St Jeor 공식 사용:
     * 남성: 10*체중(kg) + 6.25*키(cm) - 5*나이(세) + 5
     * 여성: 10*체중(kg) + 6.25*키(cm) - 5*나이(세) - 161
     *
     * 예시) 남성, 175cm, 70kg, 30세
     *      = 10*70 + 6.25*175 - 5*30 + 5
     *      = 700 + 1093.75 - 150 + 5
     *      = 1648.75 ≈ 1649 kcal
     *
     * @param request 사용자 신체 정보
     * @return 기초대사량 (kcal)
     */
    private int calculateBmr(MealPlanRequest request) {
        if ("MALE".equalsIgnoreCase(request.getGender())) {
            // 남성 공식
            return (int) Math.round(
                    10 * request.getWeight()
                            + 6.25 * request.getHeight()
                            - 5 * request.getAge()
                            + 5
            );
        }

        // 여성 공식
        return (int) Math.round(
                10 * request.getWeight()
                        + 6.25 * request.getHeight()
                        - 5 * request.getAge()
                        - 161
        );
    }

    /**
     * TDEE(일일 총 소비 칼로리) 계산
     *
     * TDEE = BMR * 활동 계수
     * 활동 계수:
     * - 1.2: 거의 운동 안 함 (Sedentary)
     * - 1.35: 가벼운 운동 (Lightly Active) - 우리는 이 값 사용
     * - 1.55: 중등도 운동 (Moderately Active)
     * - 1.725: 활발한 운동 (Very Active)
     * - 1.9: 매우 활발한 운동 (Extremely Active)
     *
     * @param bmr 기초대사량
     * @return 일일 총 소비 칼로리
     */
    private int calculateTdee(int bmr) {
        return (int) Math.round(bmr * 1.35);
    }

    /**
     * 목표에 따른 목표 칼로리 계산
     *
     * 📌 식이 조정:
     * - WEIGHT_LOSS: TDEE - 400 (주당 약 0.5kg 감량)
     * - MUSCLE_GAIN: TDEE + 300 (근육 성장을 위한 과잉 칼로리)
     * - 그 외: TDEE (현재 체중 유지)
     *
     * @param tdee 일일 총 소비 칼로리
     * @param goal 사용자 목표
     * @return 조정된 목표 칼로리
     */
    private int calculateTargetCalories(int tdee, String goal) {
        // Java 16+ 문법: switch expression (switch ... case ... yield 대신 사용)
        return switch (goal) {
            case "WEIGHT_LOSS" -> tdee - 400;
            case "MUSCLE_GAIN" -> tdee + 300;
            default -> tdee;
        };
    }

    /**
     * 단백질 그램 계산
     *
     * 📌 단백질 필요량:
     * - 일반인: 체중 당 0.8g/kg
     * - 근력 운동: 체중 당 1.6-2.2g/kg
     * - 우리는 활동적인 사용자를 가정하여 1.6g/kg 사용
     *
     * @param request 사용자 정보
     * @return 권장 단백질 그램
     */
    private int calculateProteinGram(MealPlanRequest request) {
        return (int) Math.round(request.getWeight() * 1.6);
    }

    /**
     * 지방 그램 계산
     *
     * 📌 지방 분배:
     * - 목표 칼로리의 25%를 지방으로 할당
     * - 지방 1g = 9 kcal
     * - 따라서: (칼로리 * 0.25) / 9 = 지방 그램
     *
     * @param targetCalories 목표 칼로리
     * @return 권장 지방 그램
     */
    private int calculateFatGram(int targetCalories) {
        return (int) Math.round((targetCalories * 0.25) / 9);
    }

    /**
     * 탄수화물 그램 계산
     *
     * 📌 칼로리 분배 공식:
     * 목표 칼로리 = (단백질g * 4) + (지방g * 9) + (탄수화물g * 4)
     *
     * 탄수화물g = (목표칼로리 - 단백질칼로리 - 지방칼로리) / 4
     *
     * 예시) 목표 2000kcal, 단백질 112g, 지방 56g
     *      단백질칼로리: 112 * 4 = 448
     *      지방칼로리: 56 * 9 = 504
     *      탄수화물칼로리: 2000 - 448 - 504 = 1048
     *      탄수화물g: 1048 / 4 = 262g
     *
     * @param targetCalories 목표 칼로리
     * @param proteinGram 단백질 그램
     * @param fatGram 지방 그램
     * @return 권장 탄수화물 그램
     */
    private int calculateCarbsGram(
            int targetCalories,
            int proteinGram,
            int fatGram
    ) {
        // 단백질이 제공하는 칼로리 (1g = 4 kcal)
        int proteinCalories = proteinGram * 4;

        // 지방이 제공하는 칼로리 (1g = 9 kcal)
        int fatCalories = fatGram * 9;

        // 남은 칼로리를 탄수화물로 (1g = 4 kcal)
        return (targetCalories - proteinCalories - fatCalories) / 4;
    }

    @Transactional
    public List<SavedMealPlanResponse> getSavedMealPlans(){
        User user = userRepository.findByTossUserKey(MOCK_USER_KEY)
                .orElseGet(() -> userRepository.save(new User(MOCK_USER_KEY, "test_user")));

        return mealPlanRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(SavedMealPlanResponse::from)
                .toList();
    }
}