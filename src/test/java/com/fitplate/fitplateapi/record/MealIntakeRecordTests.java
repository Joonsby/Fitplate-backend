// 식단 날짜 지정·끼니 섭취 기록과 날짜별 기록·통계 계산, 먹은 끼니 잠금을 검증합니다.
package com.fitplate.fitplateapi.record;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.ai.MealPlanAiClient;
import com.fitplate.fitplateapi.exception.InvalidRequestException;
import com.fitplate.fitplateapi.exception.MealPlanAccessDeniedException;
import com.fitplate.fitplateapi.exception.MealPlanConflictException;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.SavedMealPlanResponse;
import com.fitplate.fitplateapi.mealplan.service.MealIntakeService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanEditService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanOwnership;
import com.fitplate.fitplateapi.mealplan.service.MealPlanStorageService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanValidator;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.record.dto.DailyRecordResponse;
import com.fitplate.fitplateapi.record.dto.DailyRecordResponse.RecordStatus;
import com.fitplate.fitplateapi.record.dto.NutritionTotals;
import com.fitplate.fitplateapi.record.dto.RecordsResponse;
import com.fitplate.fitplateapi.record.service.DailyRecordService;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import com.fitplate.fitplateapi.personalization.service.PersonalizationService;
import com.fitplate.fitplateapi.aiusage.service.AiUsageQuota;
import com.fitplate.fitplateapi.mealplan.service.FoodAlternativePool;
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
import static com.fitplate.fitplateapi.mealplan.MealPlanValidatorTests.food;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({MealPlanStorageService.class, MealIntakeService.class, MealPlanOwnership.class, DailyRecordService.class,
        FoodAlternativePool.class, AiUsageQuota.class,
        MealPlanEditService.class, MealPlanValidator.class, MealIntakeRecordTests.TestBeans.class})
class MealIntakeRecordTests {
    private static final String USER_KEY = "intake-test-user";
    static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    @Autowired private MealPlanStorageService storageService;
    @Autowired private MealIntakeService intakeService;
    @Autowired private DailyRecordService recordService;
    @Autowired private MealPlanEditService editService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private MealPlanAiClient aiClient;
    @MockitoBean private UserProfileService userProfileService;
    @MockitoBean private PersonalizationService personalizationService;

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
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(FoodPreferences.none());
    }

    /** 끼니마다 700kcal · 단백질 40g · 탄수화물 90g · 지방 20g, 목표 2100kcal 인 식단을 저장한다. */
    private Long savePlan() {
        return storageService.saveGeneratedMealPlan(USER_KEY,
                MealPlanRequest.builder().height(175).weight(70).age(30).gender("MALE").goal("MAINTAIN").build(),
                NutritionResult.builder().bmr(1600).tdee(2100).targetCalories(2100)
                        .proteinGram(120).carbsGram(270).fatGram(60).build(),
                balancedPlan("연어구이"));
    }

    private DailyRecordResponse recordOf(LocalDate date) {
        return recordService.getRecords(USER_KEY, date, date).days().get(0);
    }

    @Test
    void eatenNutritionCountsOnlyCheckedMeals() {
        Long planId = savePlan();
        intakeService.schedule(USER_KEY, planId, TODAY, false);

        SavedMealPlanResponse response = intakeService.setIntake(USER_KEY, planId, "breakfast", true);

        assertThat(response.getPlanDate()).isEqualTo(TODAY);
        assertThat(response.getAiMealPlanResponse().getMeals()).filteredOn(meal -> meal.isEaten()).hasSize(1);
        DailyRecordResponse today = recordOf(TODAY);
        assertThat(today.status()).isEqualTo(RecordStatus.PARTIAL);
        assertThat(today.completionRate()).isEqualTo(1.0 / 3);
        assertThat(today.eaten()).isEqualTo(new NutritionTotals(700, 40, 90, 20));
        assertThat(today.planned()).isEqualTo(new NutritionTotals(2100, 120, 270, 60));
        assertThat(today.target()).isEqualTo(new NutritionTotals(2100, 120, 270, 60));
    }

    @Test
    void daysWithoutChecksAreNotTreatedAsZeroIntake() {
        Long todayPlan = savePlan();
        Long tomorrowPlan = savePlan();
        intakeService.schedule(USER_KEY, todayPlan, TODAY, false);
        intakeService.schedule(USER_KEY, tomorrowPlan, TODAY.plusDays(1), false);

        RecordsResponse records = recordService.getRecords(USER_KEY, TODAY.minusDays(1), TODAY.plusDays(1));

        assertThat(records.days()).extracting(DailyRecordResponse::status)
                .containsExactly(RecordStatus.NO_PLAN, RecordStatus.NOT_RECORDED, RecordStatus.UPCOMING);
        assertThat(records.days()).allSatisfy(day -> assertThat(day.eaten()).isNull());
        assertThat(records.days().get(1).completionRate()).isZero();
        assertThat(records.days().get(2).completionRate()).isNull();
        assertThat(records.summary().plannedDays()).isEqualTo(1);
        assertThat(records.summary().recordedDays()).isZero();
        assertThat(records.summary().averageEaten()).isNull();
    }

    @Test
    void averagesUseOnlyRecordedDays() {
        Long yesterdayPlan = savePlan();
        Long todayPlan = savePlan();
        intakeService.schedule(USER_KEY, yesterdayPlan, TODAY.minusDays(1), false);
        intakeService.schedule(USER_KEY, todayPlan, TODAY, false);
        for (String meal : List.of("breakfast", "lunch", "dinner")) {
            intakeService.setIntake(USER_KEY, yesterdayPlan, meal, true);
        }

        RecordsResponse records = recordService.getRecords(USER_KEY, TODAY.minusDays(2), TODAY);

        assertThat(records.summary().recordedDays()).isEqualTo(1);
        assertThat(records.summary().averageEaten()).isEqualTo(new NutritionTotals(2100, 120, 270, 60));
        assertThat(records.summary().averageCompletionRate()).isEqualTo(0.5);
        assertThat(records.summary().averagePlanned()).isEqualTo(new NutritionTotals(2100, 120, 270, 60));
        assertThat(records.summary().averageTarget()).isEqualTo(new NutritionTotals(2100, 120, 270, 60));
        assertThat(records.days().get(1).status()).isEqualTo(RecordStatus.COMPLETE);
    }

    @Test
    void intakeNeedsScheduledPastOrTodayPlanAndIsIdempotent() {
        Long planId = savePlan();
        assertThatThrownBy(() -> intakeService.setIntake(USER_KEY, planId, "lunch", true))
                .isInstanceOfSatisfying(MealPlanConflictException.class,
                        e -> assertThat(e.getCode()).isEqualTo(MealPlanConflictException.NOT_SCHEDULED));

        intakeService.schedule(USER_KEY, planId, TODAY.plusDays(1), false);
        assertThatThrownBy(() -> intakeService.setIntake(USER_KEY, planId, "lunch", true))
                .isInstanceOfSatisfying(MealPlanConflictException.class,
                        e -> assertThat(e.getCode()).isEqualTo(MealPlanConflictException.FUTURE_DATE));

        intakeService.schedule(USER_KEY, planId, TODAY, false);
        intakeService.setIntake(USER_KEY, planId, "lunch", true);
        intakeService.setIntake(USER_KEY, planId, "lunch", true);
        assertThat(recordOf(TODAY).eatenMeals()).isEqualTo(1);
        intakeService.setIntake(USER_KEY, planId, "lunch", false);
        intakeService.setIntake(USER_KEY, planId, "lunch", false);
        assertThat(recordOf(TODAY).eatenMeals()).isZero();
    }

    @Test
    void sameDateNeedsReplaceAndPlansWithIntakeCannotMove() {
        Long first = savePlan();
        Long second = savePlan();
        intakeService.schedule(USER_KEY, first, TODAY, false);

        assertThatThrownBy(() -> intakeService.schedule(USER_KEY, second, TODAY, false))
                .isInstanceOfSatisfying(MealPlanConflictException.class,
                        e -> assertThat(e.getCode()).isEqualTo(MealPlanConflictException.DATE_TAKEN));

        intakeService.schedule(USER_KEY, second, TODAY, true);
        assertThat(recordOf(TODAY).mealPlanId()).isEqualTo(second);
        assertThat(jdbc.queryForObject("SELECT plan_date FROM meal_plans WHERE meal_plan_id = ?", LocalDate.class, first))
                .isNull();

        intakeService.setIntake(USER_KEY, second, "dinner", true);
        assertThatThrownBy(() -> intakeService.schedule(USER_KEY, first, TODAY, true))
                .isInstanceOfSatisfying(MealPlanConflictException.class,
                        e -> assertThat(e.getCode()).isEqualTo(MealPlanConflictException.HAS_INTAKE));
        assertThatThrownBy(() -> intakeService.schedule(USER_KEY, second, null, false))
                .isInstanceOfSatisfying(MealPlanConflictException.class,
                        e -> assertThat(e.getCode()).isEqualTo(MealPlanConflictException.HAS_INTAKE));
    }

    @Test
    void eatenMealIsLockedAndSwappedFoodIsReflectedInRecords() {
        Long planId = savePlan();
        intakeService.schedule(USER_KEY, planId, TODAY, false);
        intakeService.setIntake(USER_KEY, planId, "lunch", true);

        assertThatThrownBy(() -> editService.replaceFood(USER_KEY, planId, "lunch", 1, food("고등어구이", 290, 28, 0, 18)))
                .isInstanceOfSatisfying(MealPlanConflictException.class,
                        e -> assertThat(e.getCode()).isEqualTo(MealPlanConflictException.MEAL_EATEN));
        assertThatThrownBy(() -> editService.regenerateMeal(USER_KEY, planId, "lunch"))
                .isInstanceOf(MealPlanConflictException.class);

        intakeService.setIntake(USER_KEY, planId, "lunch", false);
        editService.replaceFood(USER_KEY, planId, "lunch", 1, food("고등어구이", 290, 28, 0, 18));
        intakeService.setIntake(USER_KEY, planId, "lunch", true);

        assertThat(recordOf(TODAY).eaten()).isEqualTo(new NutritionTotals(690, 38, 80, 23));
    }

    @Test
    void usersCannotTouchOrSeeOthersPlans() {
        Long planId = savePlan();
        intakeService.schedule(USER_KEY, planId, TODAY, false);
        userRepository.saveAndFlush(User.create("other-user"));

        assertThatThrownBy(() -> intakeService.schedule("other-user", planId, TODAY, true))
                .isInstanceOf(MealPlanAccessDeniedException.class);
        assertThatThrownBy(() -> intakeService.setIntake("other-user", planId, "lunch", true))
                .isInstanceOf(MealPlanAccessDeniedException.class);
        assertThat(recordService.getRecords("other-user", TODAY, TODAY).days().get(0).status())
                .isEqualTo(RecordStatus.NO_PLAN);
    }

    @Test
    void rangeIsValidated() {
        assertThatThrownBy(() -> recordService.getRecords(USER_KEY, TODAY, TODAY.minusDays(1)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> recordService.getRecords(USER_KEY, TODAY.minusDays(93), TODAY))
                .isInstanceOf(InvalidRequestException.class);
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
