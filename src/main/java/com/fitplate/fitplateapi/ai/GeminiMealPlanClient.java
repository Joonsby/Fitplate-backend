package com.fitplate.fitplateapi.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import com.fitplate.fitplateapi.mealplan.dto.MealResponse;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class GeminiMealPlanClient implements MealPlanAiClient {

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
    @Override
    public MealPlanResponse generateMealPlan(MealPlanRequest request, NutritionResult nutritionResult,
                                             FoodPreferences preferences, PersonalizationHints hints, String feedback) {
        return call(
                "식단 생성",
                MealPlanPrompts.generation(request, nutritionResult, preferences, hints, feedback),
                MealPlanSchemas.mealPlan(false),
                MealPlanResponse.class
        );
    }

    /** 음식 하나를 대신할 대안 음식을 생성한다. */
    @Override
    public List<FoodItem> suggestAlternatives(FoodAlternativeContext context) {
        AiFoodAlternatives result = call(
                "음식 대안",
                MealPlanPrompts.alternatives(context),
                MealPlanSchemas.alternatives(false),
                AiFoodAlternatives.class
        );
        return result.getAlternatives() == null ? List.of() : result.getAlternatives();
    }

    /** 한 끼를 새로 구성한다. */
    @Override
    public MealResponse regenerateMeal(MealRegenerationContext context) {
        return call(
                "끼니 재생성",
                MealPlanPrompts.mealRegeneration(context),
                MealPlanSchemas.meal(false),
                MealResponse.class
        );
    }

    private <T> T call(String purpose, String prompt, Map<String, Object> schema, Class<T> type) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Gemini API 호출 시작 - {}", purpose);
            // 요청 JSON 구성 (Map → JSON 변환)
            Map<String, Object> geminiRequest = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", prompt)
                                    )
                            )
                    ),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            // 스키마로 출력 형식 강제
                            "responseSchema", schema
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

            // 응답 JSON 파싱
            JsonNode root = objectMapper.readTree(rawResponse);

            // candidates[0].content.parts[0].text 에서 결과 JSON 추출
            String jsonText = root
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            log.info("Gemini API 호출 완료 - {}. {}ms", purpose, System.currentTimeMillis() - startTime);

            return objectMapper.readValue(jsonText, type);

        } catch(HttpClientErrorException.TooManyRequests e) {
            // Rate Limit 초과
            throw new RuntimeException("AI 요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.", e);

        } catch(HttpClientErrorException.Unauthorized e) {
            // API 키 인증 실패
            throw new RuntimeException("Gemini API 인증에 실패했습니다.", e);

        } catch (Exception e) {
            // 네트워크/파싱 등 기타 예외
            log.error("Gemini {} API 호출 실패. {}ms", purpose, System.currentTimeMillis() - startTime, e);
            throw new RuntimeException("Gemini " + purpose + " API 호출 실패", e);
        }
    }
}
