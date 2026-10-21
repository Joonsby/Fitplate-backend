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
 * OpenAI API와 통신하여 식단을 생성하는 클라이언트.
 * API 호출, JSON 요청/응답 처리, 에러 처리를 담당한다.
 */
@Slf4j
@Component
public class OpenAiMealPlanClient implements MealPlanAiClient {

    // Spring HTTP 클라이언트
    private final RestClient restClient;

    // JSON 변환용
    private final ObjectMapper objectMapper;

    // OpenAI API 키 (application.yaml 주입)
    private final String apiKey;

    // OpenAI 모델 이름 (application.yaml 주입)
    private final String model;


    /**
     * 의존성 주입 생성자.
     */
    public OpenAiMealPlanClient(
            ObjectMapper objectMapper,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;

        // 기본 URL 설정
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    /**
     * OpenAI API를 호출하여 식단을 생성한다.
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
                "meal_plan_response",
                MealPlanPrompts.generation(request, nutritionResult, preferences, hints, feedback),
                MealPlanSchemas.mealPlan(true),
                MealPlanResponse.class
        );
    }

    /** 음식 하나를 대신할 대안 음식을 생성한다. */
    @Override
    public List<FoodItem> suggestAlternatives(FoodAlternativeContext context) {
        AiFoodAlternatives result = call(
                "음식 대안",
                "food_alternatives",
                MealPlanPrompts.alternatives(context),
                MealPlanSchemas.alternatives(true),
                AiFoodAlternatives.class
        );
        return result.getAlternatives() == null ? List.of() : result.getAlternatives();
    }

    /** 한 끼를 새로 구성한다. */
    @Override
    public MealResponse regenerateMeal(MealRegenerationContext context) {
        return call(
                "끼니 재생성",
                "meal_regeneration",
                MealPlanPrompts.mealRegeneration(context),
                MealPlanSchemas.meal(true),
                MealResponse.class
        );
    }

    private <T> T call(String purpose, String schemaName, String prompt, Map<String, Object> schema, Class<T> type) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("OpenAI API 호출 시작 - {}", purpose);

            // 요청 JSON 구성 (Map → JSON 변환)
            Map<String, Object> openAiRequest = Map.of(
                    "model", model,
                    "input", prompt,
                    "text", Map.of(
                            "format", Map.of(
                                    "type", "json_schema",
                                    "name", schemaName,
                                    "strict", true,
                                    "schema", schema
                            )
                    )
            );

            // HTTP POST 요청 전송
            String rawResponse = restClient.post()
                    .uri("/responses")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(openAiRequest)
                    .retrieve()
                    .body(String.class);

            // 응답 JSON 파싱
            JsonNode root = objectMapper.readTree(rawResponse);

            // output 에서 결과 JSON 추출
            String jsonText = root
                    .path("output")
                    .get(0)
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText();

            log.info("OpenAI API 호출 완료 - {}. {}ms", purpose, System.currentTimeMillis() - startTime);

            return objectMapper.readValue(jsonText, type);

        } catch(HttpClientErrorException.TooManyRequests e) {
            // Rate Limit 초과
            log.error("OpenAI 429 body={}", e.getResponseBodyAsString());
            throw e;

        } catch(HttpClientErrorException.Unauthorized e) {
            // API 키 인증 실패
            throw new RuntimeException("OpenAi API 인증에 실패했습니다.", e);

        } catch (Exception e) {
            // 네트워크/파싱 등 기타 예외
            log.error("OpenAI {} API 호출 실패. {}ms", purpose, System.currentTimeMillis() - startTime, e);
            throw new RuntimeException("OpenAi " + purpose + " API 호출 실패", e);
        }
    }
}
