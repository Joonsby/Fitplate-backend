// 관계형 식단 저장과 조회 및 저장 실패 시 전체 롤백을 검증합니다.
package com.fitplate.fitplateapi.mealplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.ai.MealPlanAiClient;
import com.fitplate.fitplateapi.mealplan.dto.*;
import com.fitplate.fitplateapi.mealplan.service.MealPlanService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanStorageService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanValidator;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.nutrition.service.NutritionCalculator;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import com.fitplate.fitplateapi.personalization.service.PersonalizationService;
import com.fitplate.fitplateapi.aiusage.service.AiUsageQuota;
import com.fitplate.fitplateapi.mealplan.service.MealIntakeService;
import com.fitplate.fitplateapi.user.service.UserProfileService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({MealPlanStorageService.class, MealPlanService.class, MealPlanValidator.class, MealPlanPersistenceTests.JsonConfiguration.class})
class MealPlanPersistenceTests {
    private static final String USER_KEY = "relational-test-user";

    @Autowired private MealPlanStorageService storageService;
    @Autowired private MealPlanService mealPlanService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @MockitoBean private MealPlanAiClient aiClient;
    @MockitoBean private UserProfileService userProfileService;
    @MockitoBean private PersonalizationService personalizationService;
    @MockitoBean private AiUsageQuota aiUsageQuota;
    @MockitoBean private MealIntakeService mealIntakeService;
    @MockitoBean private NutritionCalculator nutritionCalculator;

    @BeforeEach
    void prepareUser() {
        assertThat(jdbc.execute((ConnectionCallback<String>) connection ->
                connection.getMetaData().getDatabaseProductName())).isEqualTo("H2");
        for (String table : List.of("meal_plan_foods", "meal_plan_meals", "meal_plans",
                "favorite_foods", "user_profiles", "users")) {
            jdbc.update("DELETE FROM " + table);
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            User user = userRepository.saveAndFlush(User.create(USER_KEY));
            userProfileRepository.saveAndFlush(UserProfile.builder()
                    .user(user).height(180).weight(75).age(30).gender("MALE")
                    .bmi(new BigDecimal("23.15")).bodyFatRate(new BigDecimal("20.00"))
                    .build());
        });
    }

    @Test
    void savesPlanMealsAndFoodsTogetherWithDecimalNutrition() {
        storageService.saveGeneratedMealPlan(USER_KEY, request(), nutrition(), sample());

        assertThat(count("meal_plans")).isEqualTo(1);
        assertThat(count("meal_plan_meals")).isEqualTo(3);
        assertThat(count("meal_plan_foods")).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT protein FROM meal_plan_foods ORDER BY food_id LIMIT 1", BigDecimal.class))
                .isEqualByComparingTo("12.35");
        assertThat(jdbc.queryForObject("SELECT ai_response_json IS NOT NULL FROM meal_plans", Boolean.class))
                .isTrue();
    }

    @Test
    void listAndDetailReadRelationalDataEvenWhenJsonHasNoMeals() {
        MealPlanResponse expected = sample();
        storageService.saveGeneratedMealPlan(USER_KEY, request(), nutrition(), expected);
        Long id = jdbc.queryForObject("SELECT meal_plan_id FROM meal_plans", Long.class);
        jdbc.update("UPDATE meal_plans SET ai_response_json = ? WHERE meal_plan_id = ?", "{}", id);

        List<SavedMealPlanResponse> saved = mealPlanService.getSavedMealPlans(USER_KEY);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getAiMealPlanResponse()).usingRecursiveComparison().isEqualTo(expected);
        MealPlanDetailResponse detail = mealPlanService.findById(id);
        assertThat(detail.getAiMealPlanResponse()).usingRecursiveComparison().isEqualTo(expected);
        assertThat(detail.getTargetCalories()).isEqualTo(2200);
        assertThat(detail.getHeight()).isEqualTo(180);
    }

    @Test
    void failedChildInsertRollsBackParentAndPreviouslyInsertedChildren() {
        MealResponse breakfast = sample().getMeals().get(1);
        MealPlanResponse duplicateMeals = MealPlanResponse.builder()
                .meals(List.of(breakfast, breakfast)).build();

        assertThatThrownBy(() -> storageService.saveGeneratedMealPlan(USER_KEY, request(), nutrition(), duplicateMeals))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(count("meal_plans")).isZero();
        assertThat(count("meal_plan_meals")).isZero();
        assertThat(count("meal_plan_foods")).isZero();
        assertThat(count("users")).isEqualTo(1);
    }

    @Test
    void deletingPlanRemovesAllMealsAndFoods() {
        storageService.saveGeneratedMealPlan(USER_KEY, request(), nutrition(), sample());
        Long id = jdbc.queryForObject("SELECT meal_plan_id FROM meal_plans", Long.class);

        mealPlanService.deleteMealPlan(USER_KEY, id);

        assertThat(count("meal_plans")).isZero();
        assertThat(count("meal_plan_meals")).isZero();
        assertThat(count("meal_plan_foods")).isZero();
    }

    @Test
    void anotherUserCannotDeletePlanOrSeeItInTheirList() {
        storageService.saveGeneratedMealPlan(USER_KEY, request(), nutrition(), sample());
        Long id = jdbc.queryForObject("SELECT meal_plan_id FROM meal_plans", Long.class);
        String otherKey = "other-test-user";
        userRepository.saveAndFlush(User.create(otherKey));

        assertThat(mealPlanService.getSavedMealPlans(otherKey)).isEmpty();
        assertThatThrownBy(() -> mealPlanService.deleteMealPlan(otherKey, id))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(count("meal_plans")).isEqualTo(1);
        assertThat(count("meal_plan_foods")).isEqualTo(6);
    }

    @Test
    void generationCallsAiOutsideTransactionAndPersistsRelationalResult() {
        MealPlanRequest request = request();
        NutritionResult nutrition = nutrition();
        MealPlanResponse response = sample();
        when(nutritionCalculator.calculate(180, 75, 30, 20.0, "MALE", "MAINTAIN"))
                .thenReturn(nutrition);
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(FoodPreferences.none());
        when(aiClient.generateMealPlan(eq(request), eq(nutrition), any(), any(), any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return response;
        });

        MealPlanGenerateResponse generated = mealPlanService.generateMealPlan(USER_KEY, request);

        assertThat(generated.getAiMealPlanResponse()).usingRecursiveComparison().isEqualTo(response);
        assertThat(generated.getMealPlanId())
                .isEqualTo(jdbc.queryForObject("SELECT meal_plan_id FROM meal_plans", Long.class));
        assertThat(count("meal_plan_meals")).isEqualTo(3);
        assertThat(count("meal_plan_foods")).isEqualTo(6);
    }

    @Test
    void listingMultiplePlansBatchesFoodQueriesAndPreservesOrder() {
        storageService.saveGeneratedMealPlan(USER_KEY, request(), nutrition(), sample());
        storageService.saveGeneratedMealPlan(USER_KEY, request(), nutrition(), sample());
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<SavedMealPlanResponse> saved = mealPlanService.getSavedMealPlans(USER_KEY);

        assertThat(saved).hasSize(2);
        for (SavedMealPlanResponse plan : saved) {
            assertThat(plan.getAiMealPlanResponse()).usingRecursiveComparison().isEqualTo(sample());
        }
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private MealPlanRequest request() {
        return MealPlanRequest.builder().height(180).weight(75).age(30).bodyFatRate(20.0)
                .gender("MALE").goal("MAINTAIN").build();
    }

    private NutritionResult nutrition() {
        return NutritionResult.builder().bmr(1700).tdee(2300).targetCalories(2200)
                .proteinGram(120).carbsGram(280).fatGram(60).build();
    }

    private MealPlanResponse sample() {
        return MealPlanResponse.builder().meals(List.of(
                meal("dinner", "저녁 식단"), meal("breakfast", "아침 식단"), meal("lunch", "점심 식단")
        )).build();
    }

    private MealResponse meal(String type, String title) {
        return MealResponse.builder().mealType(type).title(title).foods(List.of(
                food("현미밥", "1공기", 300, 12.35, 45.67, 8.91),
                food("닭가슴살", "150g", 220, 30.25, 1.50, 3.75)
        )).build();
    }

    private FoodItem food(String name, String amount, int calories, double protein, double carbs, double fat) {
        return FoodItem.builder().name(name).amount(amount).calories(calories)
                .protein(protein).carbohydrate(carbs).fat(fat).shoppingKeyword(name).build();
    }

    @TestConfiguration
    static class JsonConfiguration {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
