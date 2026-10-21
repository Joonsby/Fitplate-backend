// 식단 생성 시 AI 결과 검증 실패에 따른 재요청과 결과 선택을 검증합니다.
package com.fitplate.fitplateapi.mealplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.ai.MealPlanAiClient;
import com.fitplate.fitplateapi.ai.PersonalizationHints;
import com.fitplate.fitplateapi.exception.AllergySafeResultUnavailableException;
import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanGenerateResponse;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import com.fitplate.fitplateapi.mealplan.dto.MealResponse;
import com.fitplate.fitplateapi.mealplan.service.MealPlanService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanStorageService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanValidator;
import com.fitplate.fitplateapi.nutrition.service.NutritionCalculator;
import com.fitplate.fitplateapi.user.domain.FoodCategory;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import com.fitplate.fitplateapi.personalization.service.PersonalizationService;
import com.fitplate.fitplateapi.aiusage.service.AiUsageQuota;
import com.fitplate.fitplateapi.mealplan.service.MealIntakeService;
import com.fitplate.fitplateapi.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static com.fitplate.fitplateapi.mealplan.MealPlanValidatorTests.balancedPlan;
import static com.fitplate.fitplateapi.mealplan.MealPlanValidatorTests.target;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({MealPlanStorageService.class, MealPlanService.class, MealPlanValidator.class,
        MealPlanGenerationRetryTests.JsonConfiguration.class})
class MealPlanGenerationRetryTests {
    private static final String USER_KEY = "retry-test-user";

    @Autowired private MealPlanService mealPlanService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private MealPlanAiClient aiClient;
    @MockitoBean private UserProfileService userProfileService;
    @MockitoBean private PersonalizationService personalizationService;
    @MockitoBean private AiUsageQuota aiUsageQuota;
    @MockitoBean private MealIntakeService mealIntakeService;
    @MockitoBean private NutritionCalculator nutritionCalculator;

    private final MealPlanRequest request = MealPlanRequest.builder()
            .height(175).weight(70).age(30).gender("MALE").goal("MAINTAIN").build();

    @BeforeEach
    void prepare() {
        for (String table : List.of("meal_plan_foods", "meal_plan_meals", "meal_plans",
                "favorite_foods", "user_profiles", "users")) {
            jdbc.update("DELETE FROM " + table);
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            User user = userRepository.saveAndFlush(User.create(USER_KEY));
            userProfileRepository.saveAndFlush(UserProfile.builder()
                    .user(user).height(175).weight(70).age(30).gender("MALE")
                    .bmi(new BigDecimal("22.86")).build());
        });
        when(nutritionCalculator.calculate(175, 70, 30, null, "MALE", "MAINTAIN")).thenReturn(target());
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(FoodPreferences.none());
    }

    @Test
    void acceptableFirstResultIsSavedWithoutRetry() {
        when(aiClient.generateMealPlan(eq(request), any(), any(), any(), isNull())).thenReturn(balancedPlan("연어구이"));

        MealPlanGenerateResponse response = mealPlanService.generateMealPlan(USER_KEY, request);

        verify(aiClient, times(1)).generateMealPlan(any(), any(), any(), any(), any());
        assertThat(response.getMealPlanId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo("연어구이·두부 유지 식단");
        assertThat(savedFoodNames()).contains("연어구이");
    }

    @Test
    void excludedFoodTriggersRetryWithFeedbackAndSavesCorrectedPlan() {
        FoodPreferences noPork = FoodPreferences.of(List.of(), List.of(FoodCategory.PORK));
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(noPork);
        when(aiClient.generateMealPlan(eq(request), any(), eq(noPork), any(), isNull())).thenReturn(balancedPlan("제육볶음"));
        when(aiClient.generateMealPlan(eq(request), any(), eq(noPork), any(), notNull())).thenReturn(balancedPlan("연어구이"));

        mealPlanService.generateMealPlan(USER_KEY, request);

        ArgumentCaptor<String> feedback = ArgumentCaptor.forClass(String.class);
        verify(aiClient, times(2)).generateMealPlan(any(), any(), any(), any(), feedback.capture());
        assertThat(feedback.getAllValues().get(1)).contains("제육볶음").contains("돼지고기");
        assertThat(savedFoodNames()).contains("연어구이").doesNotContain("제육볶음");
    }

    @Test
    void personalizationHintsArePassedToAi() {
        PersonalizationHints hints = new PersonalizationHints(List.of("닭가슴살"), List.of("그릭요거트"), List.of(), List.of());
        when(personalizationService.hintsFor(eq(USER_KEY), any())).thenReturn(hints);
        when(aiClient.generateMealPlan(eq(request), any(), any(), eq(hints), isNull())).thenReturn(balancedPlan("연어구이"));

        mealPlanService.generateMealPlan(USER_KEY, request);

        verify(aiClient).generateMealPlan(any(), any(), any(), eq(hints), isNull());
    }

    @Test
    void allergyFoodIsNeverSavedEvenWhenItIsCloserToTarget() {
        // 첫 결과는 목표에 딱 맞지만 알레르기(돼지고기) 음식이 있고, 두 번째는 칼로리가 모자라도 안전하다
        FoodPreferences porkAllergy = FoodPreferences.of(List.of(), List.of(), List.of(FoodCategory.PORK));
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(porkAllergy);
        when(aiClient.generateMealPlan(eq(request), any(), any(), any(), isNull())).thenReturn(balancedPlan("제육볶음"));
        when(aiClient.generateMealPlan(eq(request), any(), any(), any(), notNull()))
                .thenReturn(planWithDinner(MealPlanValidatorTests.food("귀리죽", 450, 15, 60, 10)));

        mealPlanService.generateMealPlan(USER_KEY, request);

        ArgumentCaptor<String> feedback = ArgumentCaptor.forClass(String.class);
        verify(aiClient, times(2)).generateMealPlan(any(), any(), any(), any(), feedback.capture());
        assertThat(feedback.getAllValues().get(1)).contains("알레르기").contains("제육볶음");
        assertThat(savedFoodNames()).contains("귀리죽").doesNotContain("제육볶음");
    }

    @Test
    void allergyInBothAttemptsFailsWithoutSaving() {
        FoodPreferences porkAllergy = FoodPreferences.of(List.of(), List.of(), List.of(FoodCategory.PORK));
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(porkAllergy);
        when(aiClient.generateMealPlan(eq(request), any(), any(), any(), any())).thenReturn(balancedPlan("돼지불고기"));

        assertThatThrownBy(() -> mealPlanService.generateMealPlan(USER_KEY, request))
                .isInstanceOf(AllergySafeResultUnavailableException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meal_plans", Integer.class)).isZero();
    }

    @Test
    void allergyFirstResultIsNotUsedWhenRetryFails() {
        FoodPreferences porkAllergy = FoodPreferences.of(List.of(), List.of(), List.of(FoodCategory.PORK));
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(porkAllergy);
        when(aiClient.generateMealPlan(eq(request), any(), any(), any(), isNull())).thenReturn(balancedPlan("제육볶음"));
        when(aiClient.generateMealPlan(eq(request), any(), any(), any(), notNull()))
                .thenThrow(new RuntimeException("AI 서버 오류"));

        assertThatThrownBy(() -> mealPlanService.generateMealPlan(USER_KEY, request))
                .isInstanceOf(AllergySafeResultUnavailableException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meal_plans", Integer.class)).isZero();
    }

    @Test
    void whenBothAttemptsMissTargetTheCloserOneIsSaved() {
        // 고구마 50kcal 저녁 → 하루 1450kcal(31% 부족), 귀리죽 450kcal 저녁 → 하루 1850kcal(12% 부족)
        when(aiClient.generateMealPlan(eq(request), any(), any(), any(), isNull()))
                .thenReturn(planWithDinner(MealPlanValidatorTests.food("고구마", 50, 1, 10, 0)));
        when(aiClient.generateMealPlan(eq(request), any(), any(), any(), notNull()))
                .thenReturn(planWithDinner(MealPlanValidatorTests.food("귀리죽", 450, 15, 60, 10)));

        mealPlanService.generateMealPlan(USER_KEY, request);

        assertThat(savedFoodNames()).contains("귀리죽").doesNotContain("고구마");
    }

    @Test
    void twoMalformedResultsFailWithoutSaving() {
        MealPlanResponse malformed = MealPlanResponse.builder().meals(List.of()).build();
        when(aiClient.generateMealPlan(eq(request), any(), any(), any(), any())).thenReturn(malformed);

        assertThatThrownBy(() -> mealPlanService.generateMealPlan(USER_KEY, request))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meal_plans", Integer.class)).isZero();
    }

    @Test
    void failedRetryFallsBackToWellFormedFirstResult() {
        when(aiClient.generateMealPlan(eq(request), any(), any(), any(), isNull()))
                .thenReturn(planWithDinner(MealPlanValidatorTests.food("고구마", 50, 1, 10, 0)));
        when(aiClient.generateMealPlan(eq(request), any(), any(), any(), notNull()))
                .thenThrow(new RuntimeException("AI 서버 오류"));

        mealPlanService.generateMealPlan(USER_KEY, request);

        assertThat(savedFoodNames()).contains("고구마");
    }

    private static MealPlanResponse planWithDinner(FoodItem dinnerFood) {
        return MealPlanResponse.builder().meals(List.of(
                MealPlanValidatorTests.meal("breakfast", "현미밥", "닭가슴살"),
                MealPlanValidatorTests.meal("lunch", "현미밥", "연어구이"),
                MealResponse.builder().mealType("dinner").title("dinner").foods(List.of(dinnerFood)).build()
        )).build();
    }

    private List<String> savedFoodNames() {
        return jdbc.queryForList("SELECT name FROM meal_plan_foods", String.class);
    }

    @TestConfiguration
    static class JsonConfiguration {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
