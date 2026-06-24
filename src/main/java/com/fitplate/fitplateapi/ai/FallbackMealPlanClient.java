package com.fitplate.fitplateapi.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class FallbackMealPlanClient implements MealPlanAiClient {

    private final OpenAiMealPlanClient openAiMealPlanClient;
    private final GeminiMealPlanClient geminiMealPlanClient;

    @Override
    public MealPlanResponse generateMealPlan(MealPlanRequest request, NutritionResult nutritionResult) {
        try {
            log.info("AI 식단 생성 시작 - provider=OpenAI");
            return openAiMealPlanClient.generateMealPlan(request, nutritionResult);

        } catch (HttpClientErrorException.BadRequest |
                 HttpClientErrorException.Unauthorized |
                 HttpClientErrorException.Forbidden e) {

            log.error("OpenAI 요청 오류. fallback 하지 않음. status={}", e.getStatusCode(), e);
            throw e;

        } catch (HttpClientErrorException.TooManyRequests |
                 HttpServerErrorException |
                 ResourceAccessException e) {

            log.warn("OpenAI 일시적 실패. Gemini fallback 시도. cause={}", e.getMessage());
            return callGeminiFallback(request, nutritionResult);

        } catch (RuntimeException e) {

            if (isFatalApplicationError(e)) {
                log.error("OpenAI 처리 중 애플리케이션 오류 발생. fallback 하지 않음. cause={}", e.getClass().getSimpleName(), e);
                throw e;
            }

            log.warn("OpenAI 예외 발생. Gemini fallback 시도. cause={}", e.getMessage());
            return callGeminiFallback(request, nutritionResult);
        }
    }

    private MealPlanResponse callGeminiFallback(MealPlanRequest request, NutritionResult nutritionResult) {
        try {
            log.info("AI 식단 생성 fallback 시작 - provider=Gemini");
            return geminiMealPlanClient.generateMealPlan(request, nutritionResult);
        } catch (Exception e) {
            log.error("Gemini fallback 실패", e);
            throw new RuntimeException("현재 AI 서버 사용량이 많습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    private boolean isFatalApplicationError(Throwable e) {
        Throwable current = e;

        while (current != null) {
            if (current instanceof NullPointerException ||
                    current instanceof JsonProcessingException ||
                    current instanceof IllegalArgumentException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}