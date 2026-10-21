package com.fitplate.fitplateapi.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import com.fitplate.fitplateapi.mealplan.dto.MealResponse;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class FallbackMealPlanClient implements MealPlanAiClient {

    private final OpenAiMealPlanClient openAiMealPlanClient;
    private final GeminiMealPlanClient geminiMealPlanClient;

    @Override
    public MealPlanResponse generateMealPlan(MealPlanRequest request, NutritionResult nutritionResult,
                                             FoodPreferences preferences, PersonalizationHints hints, String feedback) {
        return withFallback(
                "식단 생성",
                () -> openAiMealPlanClient.generateMealPlan(request, nutritionResult, preferences, hints, feedback),
                () -> geminiMealPlanClient.generateMealPlan(request, nutritionResult, preferences, hints, feedback)
        );
    }

    @Override
    public List<FoodItem> suggestAlternatives(FoodAlternativeContext context) {
        return withFallback(
                "음식 대안",
                () -> openAiMealPlanClient.suggestAlternatives(context),
                () -> geminiMealPlanClient.suggestAlternatives(context)
        );
    }

    @Override
    public MealResponse regenerateMeal(MealRegenerationContext context) {
        return withFallback(
                "끼니 재생성",
                () -> openAiMealPlanClient.regenerateMeal(context),
                () -> geminiMealPlanClient.regenerateMeal(context)
        );
    }

    private <T> T withFallback(String purpose, Supplier<T> openAi, Supplier<T> gemini) {
        try {
            log.info("AI {} 시작 - provider=OpenAI", purpose);
            return openAi.get();

        } catch (HttpClientErrorException.BadRequest |
                 HttpClientErrorException.Unauthorized |
                 HttpClientErrorException.Forbidden e) {

            log.error("OpenAI 요청 오류. fallback 하지 않음. status={}", e.getStatusCode(), e);
            throw e;

        } catch (HttpClientErrorException.TooManyRequests |
                 HttpServerErrorException |
                 ResourceAccessException e) {

            log.warn("OpenAI 일시적 실패. Gemini fallback 시도. cause={}", e.getMessage());
            return callGeminiFallback(purpose, gemini);

        } catch (RuntimeException e) {

            if (isFatalApplicationError(e)) {
                log.error("OpenAI 처리 중 애플리케이션 오류 발생. fallback 하지 않음. cause={}", e.getClass().getSimpleName(), e);
                throw e;
            }

            log.warn("OpenAI 예외 발생. Gemini fallback 시도. cause={}", e.getMessage());
            return callGeminiFallback(purpose, gemini);
        }
    }

    private <T> T callGeminiFallback(String purpose, Supplier<T> gemini) {
        try {
            log.info("AI {} fallback 시작 - provider=Gemini", purpose);
            return gemini.get();
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
