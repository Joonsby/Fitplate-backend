// 식단 생성의 하루 상한·첫 생성 무료 판정, 만들면서 날짜 지정, 저장 식단을 다른 날짜로 복사하는 규칙을 검증합니다.
package com.fitplate.fitplateapi.mealplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.ai.MealPlanAiClient;
import com.fitplate.fitplateapi.aiusage.domain.AiFeature;
import com.fitplate.fitplateapi.aiusage.service.AiUsageQuota;
import com.fitplate.fitplateapi.exception.AiDailyLimitExceededException;
import com.fitplate.fitplateapi.exception.InvalidRequestException;
import com.fitplate.fitplateapi.exception.MealPlanAccessDeniedException;
import com.fitplate.fitplateapi.exception.MealPlanConflictException;
import com.fitplate.fitplateapi.mealplan.dto.GenerationQuotaResponse;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.SavedMealPlanResponse;
import com.fitplate.fitplateapi.mealplan.service.MealIntakeService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanOwnership;
import com.fitplate.fitplateapi.mealplan.service.MealPlanService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanStorageService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanValidator;
import com.fitplate.fitplateapi.nutrition.service.NutritionCalculator;
import com.fitplate.fitplateapi.personalization.service.PersonalizationService;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import com.fitplate.fitplateapi.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static com.fitplate.fitplateapi.mealplan.MealPlanValidatorTests.balancedPlan;
import static com.fitplate.fitplateapi.mealplan.MealPlanValidatorTests.target;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({MealPlanService.class, MealPlanStorageService.class, MealPlanValidator.class, MealIntakeService.class,
        MealPlanOwnership.class, AiUsageQuota.class, MealPlanCopyAndQuotaTests.TestBeans.class})
class MealPlanCopyAndQuotaTests {
    private static final String USER_KEY = "copy-quota-user";
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    @Autowired private MealPlanService mealPlanService;
    @Autowired private MealIntakeService intakeService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private MealPlanAiClient aiClient;
    @MockitoBean private UserProfileService userProfileService;
    @MockitoBean private NutritionCalculator nutritionCalculator;
    @MockitoBean private PersonalizationService personalizationService;

    private final MealPlanRequest request = MealPlanRequest.builder()
            .height(175).weight(70).age(30).gender("MALE").goal("MAINTAIN").build();

    @BeforeEach
    void prepare() {
        for (String table : List.of("ai_daily_usage", "meal_plan_foods", "meal_plan_meals", "meal_plans",
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
        when(aiClient.generateMealPlan(any(), any(), any(), any(), any())).thenReturn(balancedPlan("연어구이"));
    }

    @Test
    void firstGenerationOfTheDayNeedsNoAdAndLimitIsEnforced() {
        GenerationQuotaResponse fresh = mealPlanService.getGenerationQuota(USER_KEY);
        assertThat(fresh.adRequired()).isFalse();
        assertThat(fresh.remaining()).isEqualTo(AiFeature.GENERATION.dailyLimit());

        mealPlanService.generateMealPlan(USER_KEY, request);
        GenerationQuotaResponse afterFirst = mealPlanService.getGenerationQuota(USER_KEY);
        assertThat(afterFirst.adRequired()).isTrue();
        assertThat(afterFirst.usedToday()).isEqualTo(1);

        for (int i = 1; i < AiFeature.GENERATION.dailyLimit(); i++) {
            mealPlanService.generateMealPlan(USER_KEY, request);
        }
        assertThat(mealPlanService.getGenerationQuota(USER_KEY).remaining()).isZero();
        assertThatThrownBy(() -> mealPlanService.generateMealPlan(USER_KEY, request))
                .isInstanceOf(AiDailyLimitExceededException.class)
                .hasMessageContaining("다른 날");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meal_plans", Integer.class))
                .isEqualTo(AiFeature.GENERATION.dailyLimit());
    }

    @Test
    void failedGenerationDoesNotCount() {
        when(aiClient.generateMealPlan(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("AI 서버 오류"));

        assertThatThrownBy(() -> mealPlanService.generateMealPlan(USER_KEY, request)).isInstanceOf(RuntimeException.class);

        GenerationQuotaResponse quota = mealPlanService.getGenerationQuota(USER_KEY);
        assertThat(quota.usedToday()).isZero();
        assertThat(quota.adRequired()).isFalse();
    }

    @Test
    void generationCanScheduleTheNewPlanRightAway() {
        MealPlanRequest dated = MealPlanRequest.builder()
                .height(175).weight(70).age(30).gender("MALE").goal("MAINTAIN").planDate(TODAY.plusDays(1)).build();

        var response = mealPlanService.generateMealPlan(USER_KEY, dated);

        assertThat(response.getPlanDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(jdbc.queryForObject("SELECT plan_date FROM meal_plans WHERE meal_plan_id = ?", LocalDate.class,
                response.getMealPlanId())).isEqualTo(TODAY.plusDays(1));
    }

    @Test
    void takenDateIsRejectedBeforeCallingAiUnlessReplacing() {
        MealPlanRequest dated = MealPlanRequest.builder()
                .height(175).weight(70).age(30).gender("MALE").goal("MAINTAIN").planDate(TODAY).build();
        Long first = mealPlanService.generateMealPlan(USER_KEY, dated).getMealPlanId();

        assertThatThrownBy(() -> mealPlanService.generateMealPlan(USER_KEY, dated))
                .isInstanceOf(MealPlanConflictException.class)
                .extracting("code").isEqualTo(MealPlanConflictException.DATE_TAKEN);
        // 거절은 AI 를 부르기 전이라 생성 횟수가 늘지 않는다
        assertThat(mealPlanService.getGenerationQuota(USER_KEY).usedToday()).isEqualTo(1);

        MealPlanRequest replacing = MealPlanRequest.builder()
                .height(175).weight(70).age(30).gender("MALE").goal("MAINTAIN").planDate(TODAY).replaceDate(true).build();
        Long second = mealPlanService.generateMealPlan(USER_KEY, replacing).getMealPlanId();

        assertThat(jdbc.queryForObject("SELECT plan_date FROM meal_plans WHERE meal_plan_id = ?", LocalDate.class, second))
                .isEqualTo(TODAY);
        assertThat(jdbc.queryForObject("SELECT plan_date FROM meal_plans WHERE meal_plan_id = ?", LocalDate.class, first))
                .isNull();
    }

    @Test
    void copyCreatesNewPlanOnAnotherDateAndKeepsOriginal() {
        Long sourceId = mealPlanService.generateMealPlan(USER_KEY, request).getMealPlanId();
        intakeService.schedule(USER_KEY, sourceId, TODAY, false);
        intakeService.setIntake(USER_KEY, sourceId, "breakfast", true);

        SavedMealPlanResponse copy = intakeService.copyToDate(USER_KEY, sourceId, TODAY.plusDays(1), false);

        assertThat(copy.getId()).isNotEqualTo(sourceId);
        assertThat(copy.getPlanDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(copy.getTitle()).isEqualTo("연어구이·두부 유지 식단");
        assertThat(copy.getAiMealPlanResponse().getMeals()).hasSize(3)
                .allSatisfy(meal -> assertThat(meal.isEaten()).isFalse());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meal_plan_foods", Integer.class)).isEqualTo(12);
        assertThat(jdbc.queryForObject("SELECT plan_date FROM meal_plans WHERE meal_plan_id = ?", LocalDate.class, sourceId))
                .isEqualTo(TODAY);
        // 복사는 AI 를 부르지 않으므로 생성 횟수가 늘지 않는다
        assertThat(mealPlanService.getGenerationQuota(USER_KEY).usedToday()).isEqualTo(1);
    }

    @Test
    void copyFollowsDateConflictRules() {
        Long first = mealPlanService.generateMealPlan(USER_KEY, request).getMealPlanId();
        Long second = mealPlanService.generateMealPlan(USER_KEY, request).getMealPlanId();
        intakeService.schedule(USER_KEY, second, TODAY.plusDays(2), false);

        assertThatThrownBy(() -> intakeService.copyToDate(USER_KEY, first, TODAY.plusDays(2), false))
                .isInstanceOf(MealPlanConflictException.class)
                .extracting("code").isEqualTo(MealPlanConflictException.DATE_TAKEN);

        SavedMealPlanResponse copy = intakeService.copyToDate(USER_KEY, first, TODAY.plusDays(2), true);
        assertThat(copy.getPlanDate()).isEqualTo(TODAY.plusDays(2));
        assertThat(jdbc.queryForObject("SELECT plan_date FROM meal_plans WHERE meal_plan_id = ?", LocalDate.class, second))
                .isNull();
    }

    @Test
    void copyRejectsMissingOrSameDateAndOtherUsers() {
        Long sourceId = mealPlanService.generateMealPlan(USER_KEY, request).getMealPlanId();
        intakeService.schedule(USER_KEY, sourceId, TODAY, false);

        assertThatThrownBy(() -> intakeService.copyToDate(USER_KEY, sourceId, null, false))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> intakeService.copyToDate(USER_KEY, sourceId, TODAY, false))
                .isInstanceOf(InvalidRequestException.class);
        userRepository.saveAndFlush(User.create("copy-other-user"));
        assertThatThrownBy(() -> intakeService.copyToDate("copy-other-user", sourceId, TODAY.plusDays(1), false))
                .isInstanceOf(MealPlanAccessDeniedException.class);
        verify(aiClient, never()).suggestAlternatives(any());
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        Clock clock() {
            ZoneId seoul = ZoneId.of("Asia/Seoul");
            return Clock.fixed(ZonedDateTime.of(TODAY.atTime(12, 0), seoul).toInstant(), seoul);
        }
    }
}
