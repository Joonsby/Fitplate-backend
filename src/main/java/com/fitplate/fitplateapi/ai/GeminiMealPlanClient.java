package com.fitplate.fitplateapi.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Gemini AI API와 통신하여 식단을 생성하는 클라이언트.
 * API 호출, JSON 요청/응답 처리, 에러 처리를 담당한다.
 */
@Component
public class GeminiMealPlanClient {

    // Spring HTTP 클라이언트
    private final RestClient restClient;

    // JSON 변환용
    private final ObjectMapper objectMapper;

    // Gemini API 키 (application.yaml 주입)
    private final String apiKey;

    // Gemini 모델 이름 (application.yaml 주입)
    private final String model;

    /**
     * 의존성 주입 생성자.
     */
    public GeminiMealPlanClient(
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;

        // 기본 URL 설정
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    /**
     * Gemini AI API를 호출하여 식단을 생성한다.
     *
     * @param request 사용자의 신체 정보 및 목표
     * @return AI가 생성한 식단 계획
     * @throws RuntimeException API 호출 실패 시
     */
    public MealPlanResponse generateMealPlan(MealPlanRequest request, NutritionResult nutritionResult) {
        try {
            // 요청 JSON 구성 (Map → JSON 변환)
            Map<String, Object> geminiRequest = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of(
                                                    "text", buildPrompt(request, nutritionResult)
                                            )
                                    )
                            )
                    ),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            // 스키마로 출력 형식 강제
                            "responseSchema", buildMealPlanSchema()
                    )
            );

            // HTTP POST 요청 전송
            String rawResponse = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(geminiRequest)
                    .retrieve()
                    .body(String.class);

            // 디버깅용 로그 (운영 환경에서는 제거)
            System.out.println("Gemini Response: " + rawResponse + "\n");

            // 응답 JSON 파싱
            JsonNode root = objectMapper.readTree(rawResponse);

            // candidates[0].content.parts[0].text 에서 식단 JSON 추출
            String jsonText = root
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // MealPlanResponse로 역직렬화
            return objectMapper.readValue(jsonText, MealPlanResponse.class);

        } catch(HttpClientErrorException.TooManyRequests e) {
            // Rate Limit 초과
            throw new RuntimeException("AI 요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.", e);

        } catch(HttpClientErrorException.Unauthorized e) {
            // API 키 인증 실패
            throw new RuntimeException("Gemini API 인증에 실패했습니다.", e);

        } catch (Exception e) {
            // 네트워크/파싱 등 기타 예외
            throw new RuntimeException("Gemini 식단 생성 API 호출 실패", e);
        }
    }

    /**
     * Gemini AI에게 전달할 프롬프트를 생성한다.
     * AI 역할/제약조건과 사용자 정보를 포함하며 JSON만 반환하도록 지시한다.
     *
     * @param request 사용자 정보
     * @return 생성된 프롬프트 문자열
     */
    private String buildPrompt(MealPlanRequest request, NutritionResult nutritionResult) {
        return """
            너는 전문 식단 추천 API다.

            사용자의 신체 정보와 목표를 바탕으로 한국식 식단을 생성해라.

            반드시 아래 JSON 구조만 반환해라.
            설명 문장, 마크다운, 코드블록은 절대 반환하지 마라.

            조건:
            - durationDays 개수만큼 days 배열을 생성해라.
            - 하루에는 breakfast, lunch, dinner 3끼를 모두 포함해라.
            - mealType은 반드시 breakfast, lunch, dinner 중 하나만 사용해라.
            - 음식명은 "현미밥과 닭가슴살 미역국"처럼 합치지 말고 반드시 개별 음식으로 분리해라.
            - 각 음식은 name, amount, calories, protein, carbohydrate, fat, shoppingKeyword를 포함해라.
            - shoppingKeyword는 쿠팡 같은 쇼핑몰에서 검색하기 좋은 짧은 키워드로 작성해라.
            - 각 끼니의 총 칼로리는 목표 칼로리에 맞게 구성해라.
            - 하루 총 칼로리는 targetCalories의 ±10%% 이내로 맞춰라.
            - 하루 총 단백질은 proteinGram의 ±10%% 이내로 맞춰라.
            - 하루 총 탄수화물은 carbsGram의 ±10%% 이내로 맞춰라.
            - 하루 총 지방은 fatGram의 ±10%% 이내로 맞춰라.
            - goal이 WEIGHT_LOSS면 감량식으로 구성해라.
            - 한국에서 일반적으로 구하기 쉬운 식재료 위주로 구성해라.
            - foods 배열의 각 원소는 반드시 하나의 음식만 표현해야 한다.
            잘못된 예:
            {
              "name": "현미밥, 닭가슴살, 미역국"
            }
            
            올바른 예:
            {
              "name": "현미밥"
            },
            {
              "name": "닭가슴살"
            },
            {
              "name": "미역국"
            }

            반환 JSON 예시:
            {
              "days": [
                {
                  "dayNumber": 1,
                  "meals": [
                    {
                      "mealType": "breakfast",
                      "title": "아침",
                      "foods": [
                        {
                          "name": "현미밥",
                          "amount": "150g",
                          "calories": 230,
                          "protein": 5,
                          "carbohydrate": 48,
                          "fat": 2,
                          "shoppingKeyword": "현미밥 즉석밥"
                        }
                      ]
                    }
                  ]
                }
              ]
            }

            사용자 정보:
            height=%s
            weight=%s
            gender=%s
            age=%s
            bodyFatRate=%s
            goal=%s
            durationDays=%s
            targetCalories=%s
            proteinGram=%s
            carbsGram=%s
            fatGram=%s
            """.formatted(
                request.getHeight(),
                request.getWeight(),
                request.getGender(),
                request.getAge(),
                request.getBodyFatRate(),
                request.getGoal(),
                request.getDurationDays(),
                nutritionResult.getTargetCalories(),
                nutritionResult.getProteinGram(),
                nutritionResult.getCarbsGram(),
                nutritionResult.getFatGram()
        );
    }

    /**
     * AI 응답 JSON 스키마를 정의한다. AI가 정확한 형식으로 생성하도록 강제한다.
     *
     * @return JSON 스키마 Map
     */
    private Map<String, Object> buildMealPlanSchema() {
        Map<String, Object> foodSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("type", "string"),
                        "amount", Map.of("type", "string"),
                        "calories", Map.of("type", "integer"),
                        "protein", Map.of("type", "number"),
                        "carbohydrate", Map.of("type", "number"),
                        "fat", Map.of("type", "number"),
                        "shoppingKeyword", Map.of("type", "string")
                ),
                "required", List.of(
                        "name",
                        "amount",
                        "calories",
                        "protein",
                        "carbohydrate",
                        "fat",
                        "shoppingKeyword"
                )
        );

        Map<String, Object> mealSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "mealType", Map.of(
                                "type", "string",
                                "enum", List.of("breakfast", "lunch", "dinner")
                        ),
                        "title", Map.of("type", "string"),
                        "foods", Map.of(
                                "type", "array",
                                "items", foodSchema
                        )
                ),
                "required", List.of("mealType", "title", "foods")
        );

        Map<String, Object> daySchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "dayNumber", Map.of("type", "integer"),
                        "meals", Map.of(
                                "type", "array",
                                "items", mealSchema
                        )
                ),
                "required", List.of("dayNumber", "meals")
        );

        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "days", Map.of(
                                "type", "array",
                                "items", daySchema
                        )
                ),
                "required", List.of("days")
        );
    }
}