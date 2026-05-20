package com.fitplate.fitplateapi.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini AI API와 통신하여 식단을 생성하는 클라이언트
 *
 * 📌 역할:
 * - Gemini AI API 호출
 * - JSON 요청/응답 처리
 * - 에러 처리
 *
 * 📌 데이터 흐름:
 * Service → GeminiMealPlanClient
 * → HTTP POST 요청을 Gemini API로 전송
 * → AI가 사용자 정보로부터 식단 생성
 * → JSON 응답으로 그 결과 반환
 * → Service로 반환
 *
 * 📌 API 문서:
 * https://ai.google.dev/api/rest/v1beta/models/generateContent
 */
@Component  // Spring이 자동으로 빈 생성하고 의존성 주입
public class GeminiMealPlanClient {

    // RestClient: Spring에서 HTTP 요청을 보내기 위한 도구
    // (기존의 RestTemplate보다 더 현대적인 방식)
    private final RestClient restClient;

    // ObjectMapper: Jackson 라이브러리로 JSON 변환
    private final ObjectMapper objectMapper;

    // Gemini API 키 (application.yaml에서 주입됨)
    // 예: "AIzaSyD1234567890..."
    private final String apiKey;

    // 사용할 Gemini 모델 이름 (application.yaml에서 주입됨)
    // 예: "gemini-1.5-flash"
    private final String model;

    /**
     * 생성자 - 의존성 주입
     *
     * @param objectMapper Spring이 자동으로 주입하는 ObjectMapper 빈
     * @param apiKey application.yaml의 gemini.api-key 값
     * @param model application.yaml의 gemini.model 값
     */
    public GeminiMealPlanClient(
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;

        // RestClient 초기화: Gemini API의 기본 URL 설정
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    /**
     * Gemini AI API를 호출하여 식단을 생성합니다
     *
     * 📌 실행 단계:
     * 1. 사용자 정보로부터 프롬프트 생성
     * 2. API 요청 JSON 구성
     * 3. HTTP POST 요청 전송
     * 4. JSON 응답 파싱
     * 5. 에러 처리
     *
     * 📌 요청 형식:
     * {
     *   "contents": [
     *     {
     *       "parts": [
     *         {
     *           "text": "프롬프트 텍스트..."
     *         }
     *       ]
     *     }
     *   ],
     *   "generationConfig": {
     *     "responseMimeType": "application/json",
     *     "responseSchema": {...}
     *   }
     * }
     *
     * @param request 사용자의 신체 정보 및 목표
     * @return AI가 생성한 식단 계획
     * @throws RuntimeException API 호출 실패 시
     */
    public MealPlanResponse generateMealPlan(MealPlanRequest request) {
        try {
            // ============ Step 1: Gemini API 요청 JSON 구성 ============

            // Map으로 중첩된 JSON 구조 생성
            // Java의 Map이 JSON 형식으로 변환됨
            Map<String, Object> geminiRequest = Map.of(
                    // API 요청의 메인 부분
                    "contents", List.of(
                            Map.of(
                                    // contents[0].parts: 프롬프트 리스트
                                    "parts", List.of(
                                            Map.of(
                                                    // parts[0].text: 실제 프롬프트
                                                    "text", buildPrompt(request)
                                            )
                                    )
                            )
                    ),
                    // 응답 형식 설정
                    "generationConfig", Map.of(
                            // 응답을 JSON 형식으로 받겠다
                            "responseMimeType", "application/json",
                            // JSON의 스키마 정의 (AI가 정확한 형식으로 생성하도록 강제)
                            "responseSchema", buildMealPlanSchema()
                    )
            );

            // ============ Step 2: HTTP POST 요청 전송 ============

            String rawResponse = restClient.post()
                    // 엔드포인트: /models/{model}:generateContent
                    // 예: /models/gemini-1.5-flash:generateContent
                    .uri("/models/{model}:generateContent", model)
                    // API 인증 헤더
                    .header("x-goog-api-key", apiKey)
                    // 요청/응답 형식: JSON
                    .contentType(MediaType.APPLICATION_JSON)
                    // 요청 본문: 위에서 구성한 JSON
                    .body(geminiRequest)
                    // HTTP 요청 실행
                    .retrieve()
                    // 응답 본문을 문자열로 받음
                    .body(String.class);

            // 디버깅용 로그 출력 (운영 환경에서는 제거)
            System.out.println("Gemini Response: " + rawResponse + "\n");

            // ============ Step 3: 응답 JSON 파싱 ============

            // 응답 JSON을 파싱
            // 예:
            // {
            //   "candidates": [
            //     {
            //       "content": {
            //         "parts": [
            //           {
            //             "text": "{\"days\": [...]}"  <- 이 부분이 우리가 원하는 데이터
            //           }
            //         ]
            //       }
            //     }
            //   ]
            // }
            JsonNode root = objectMapper.readTree(rawResponse);

            // 응답에서 실제 식단 JSON 추출
            // candidates[0].content.parts[0].text
            String jsonText = root
                    .path("candidates")  // 응답 후보 배열
                    .get(0)              // 첫 번째 후보
                    .path("content")     // 콘텐츠 객체
                    .path("parts")       // 파트 배열
                    .get(0)              // 첫 번째 파트
                    .path("text")        // 텍스트 필드
                    .asText();           // 문자열로 변환

            // ============ Step 4: MealPlanResponse로 역직렬화 ============

            // JSON 문자열을 MealPlanResponse 객체로 변환
            // 이 객체에는 days 리스트가 포함되어 있음
            return objectMapper.readValue(jsonText, MealPlanResponse.class);

        } catch(HttpClientErrorException.TooManyRequests e) {
            // API 호출 횟수 초과 (Rate Limit)
            throw new RuntimeException("AI 요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.", e);

        } catch(HttpClientErrorException.Unauthorized e) {
            // API 키 인증 실패
            throw new RuntimeException("Gemini API 인증에 실패했습니다.", e);

        } catch (Exception e) {
            // 기타 예외 (네트워크 오류, JSON 파싱 오류 등)
            throw new RuntimeException("Gemini 식단 생성 API 호출 실패", e);
        }
    }

    /**
     * Gemini AI에게 전달할 프롬프트(지시)를 생성합니다
     *
     * 📌 프롬프트의 역할:
     * - AI에게 어떤 작업을 하도록 지시
     * - 출력 형식 지정 (JSON만 반환하도록)
     * - 사용자 정보 포함
     *
     * 📌 프롬프트 내용:
     * 1. AI의 역할: "너는 전문 식단 추천 API다"
     * 2. 작업: "사용자의 신체 정보와 목표를 바탕으로 식단을 생성해라"
     * 3. 제약조건:
     *    - 한국식 식단 (중요)
     *    - 아침, 점심, 저녁 포함
     *    - 각 끼니마다 영양 정보 포함
     *    - JSON만 반환 (설명, 마크다운 불허)
     * 4. 사용자 정보: height, weight, gender, age, goal, periodDays
     *
     * 📌 예시 프롬프트:
     * 너는 전문 식단 추천 API다.
     *
     * 사용자의 신체 정보와 목표를 바탕으로 식단을 생성해라.
     *
     * 조건:
     * - 반드시 한국식 식단 위주로 구성
     * - 아침, 점심, 저녁을 모두 포함
     * - 각 끼니는 음식명, 칼로리, 단백질, 탄수화물, 지방을 포함
     * - 목표가 WEIGHT_LOSS면 감량식으로 구성
     * - 설명 문장, 마크다운, 코드블록은 절대 반환하지 마라
     * - 오직 JSON만 반환해라
     *
     * 사용자 정보:
     * height=175
     * weight=70
     * gender=MALE
     * age=30
     * bodyFatRate=20.5
     * goal=MUSCLE_GAIN
     * periodDays=7
     *
     * @param request 사용자 정보
     * @return 생성된 프롬프트 문자열
     */
    private String buildPrompt(MealPlanRequest request) {
        return """
                너는 전문 식단 추천 API다.

                사용자의 신체 정보와 목표를 바탕으로 식단을 생성해라.

                조건:
                - 반드시 한국식 식단 위주로 구성
                - 아침, 점심, 저녁을 모두 포함
                - 각 끼니는 음식명, 칼로리, 단백질, 탄수화물, 지방을 포함
                - 목표가 WEIGHT_LOSS면 감량식으로 구성
                - 설명 문장, 마크다운, 코드블록은 절대 반환하지 마라
                - 오직 JSON만 반환해라

                사용자 정보:
                height=%s
                weight=%s
                gender=%s
                age=%s
                bodyFatRate=%s
                goal=%s
                periodDays=%s
                """.formatted(
                request.getHeight(),
                request.getWeight(),
                request.getGender(),
                request.getAge(),
                request.getBodyFatRate(),
                request.getGoal(),
                request.getPeriodDays()
        );
    }

    /**
     * Gemini AI의 응답 JSON 스키마를 정의합니다
     *
     * 📌 스키마의 역할:
     * - AI가 정확한 형식의 JSON을 생성하도록 강제
     * - 응답의 필드명, 타입을 엄격하게 정의
     * - 잘못된 형식의 응답 방지
     *
     * 📌 스키마 구조:
     * MealResponse (한 끼니):
     * {
     *   "name": "string",           // 예: "계란말이, 흰쌀밥, 미역국"
     *   "calories": "integer",      // 예: 400
     *   "protein": "number",        // 예: 25.5
     *   "carbohydrate": "number",   // 예: 45.0
     *   "fat": "number"             // 예: 12.0
     * }
     *
     * MealDayResponse (하루 식단):
     * {
     *   "dayNumber": "integer",              // 예: 1
     *   "breakfast": MealResponse,           // 아침 식사
     *   "lunch": MealResponse,               // 점심 식사
     *   "dinner": MealResponse               // 저녁 식사
     * }
     *
     * MealPlanResponse (전체 식단):
     * {
     *   "days": [                            // 일별 식단 배열
     *     MealDayResponse,
     *     MealDayResponse,
     *     ...
     *   ]
     * }
     *
     * 📌 JSON 예시 (7일 식단):
     * {
     *   "days": [
     *     {
     *       "dayNumber": 1,
     *       "breakfast": {
     *         "name": "계란말이, 흰쌀밥, 미역국",
     *         "calories": 400,
     *         "protein": 25.5,
     *         "carbohydrate": 45.0,
     *         "fat": 12.0
     *       },
     *       "lunch": {
     *         "name": "불고기덮밥, 미소된장국",
     *         "calories": 650,
     *         "protein": 35.0,
     *         "carbohydrate": 70.0,
     *         "fat": 20.0
     *       },
     *       "dinner": {
     *         "name": "생선까스, 감자튀김, 샐러드",
     *         "calories": 550,
     *         "protein": 42.0,
     *         "carbohydrate": 45.0,
     *         "fat": 15.0
     *       }
     *     },
     *     ...
     *   ]
     * }
     *
     * @return JSON 스키마 Map
     */
    private Map<String, Object> buildMealPlanSchema() {
        // ============ Step 1: 한 끼니(MealResponse)의 스키마 ============

        Map<String, Object> mealSchema = Map.of(
                "type", "object",  // 이것은 객체(JSON object)이다
                "properties", Map.of(
                        // 음식 이름 필드
                        "name", Map.of("type", "string"),
                        // 칼로리 필드 (정수)
                        "calories", Map.of("type", "integer"),
                        // 단백질 필드 (소수점 포함)
                        "protein", Map.of("type", "number"),
                        // 탄수화물 필드
                        "carbohydrate", Map.of("type", "number"),
                        // 지방 필드
                        "fat", Map.of("type", "number")
                ),
                // 필수 필드 (꼭 포함되어야 함)
                "required", List.of("name", "calories", "protein", "carbohydrate", "fat")
        );

        // ============ Step 2: 하루 식단(MealDayResponse)의 스키마 ============

        Map<String, Object> daySchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        // 날짜 번호 (1, 2, 3, ...)
                        "dayNumber", Map.of("type", "integer"),
                        // 아침 식사 (위에서 정의한 mealSchema 사용)
                        "breakfast", mealSchema,
                        // 점심 식사
                        "lunch", mealSchema,
                        // 저녁 식사
                        "dinner", mealSchema
                ),
                // 필수 필드
                "required", List.of("dayNumber", "breakfast", "lunch", "dinner")
        );

        // ============ Step 3: 전체 식단(MealPlanResponse)의 스키마 ============

        return Map.of(
                "type", "object",
                "properties", Map.of(
                        // days: 배열 (여러 개의 하루 식단)
                        "days", Map.of(
                                "type", "array",  // 배열이다
                                "items", daySchema  // 배열의 각 요소는 daySchema를 따름
                        )
                ),
                // 필수 필드: days는 꼭 있어야 함
                "required", List.of("days")
        );
    }
}