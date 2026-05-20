package com.fitplate.fitplateapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.dto.MealPlanRequest;
import com.fitplate.fitplateapi.dto.MealPlanResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeminiMealPlanClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiMealPlanClient(
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public MealPlanResponse generateMealPlan(MealPlanRequest request) {
        try {
            Map<String, Object> geminiRequest = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", buildPrompt(request))
                            ))
                    ),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "responseSchema", buildMealPlanSchema()
                    )
            );

            String rawResponse = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(geminiRequest)
                    .retrieve()
                    .body(String.class);

            System.out.println("Gemini Response: " + rawResponse + "n");

            JsonNode root = objectMapper.readTree(rawResponse);
            String jsonText = root
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            return objectMapper.readValue(jsonText, MealPlanResponse.class);

        } catch(HttpClientErrorException.TooManyRequests e) {
            throw new RuntimeException("AI 요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.", e);
        } catch(HttpClientErrorException.Unauthorized e) {
            throw new RuntimeException( "Gemini API 인증에 실패했습니다.", e);
        } catch (Exception e) {
            throw new RuntimeException("Gemini 식단 생성 API 호출 실패", e);
        }
    }

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

    private Map<String, Object> buildMealPlanSchema() {
        Map<String, Object> mealSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("type", "string"),
                        "calories", Map.of("type", "integer"),
                        "protein", Map.of("type", "number"),
                        "carbohydrate", Map.of("type", "number"),
                        "fat", Map.of("type", "number")
                ),
                "required", List.of("name", "calories", "protein", "carbohydrate", "fat")
        );

        Map<String, Object> daySchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "dayNumber", Map.of("type", "integer"),
                        "breakfast", mealSchema,
                        "lunch", mealSchema,
                        "dinner", mealSchema
                ),
                "required", List.of("dayNumber", "breakfast", "lunch", "dinner")
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