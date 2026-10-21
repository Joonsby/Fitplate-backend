// 음식 대안 조회의 후보 걸러내기·즐겨찾기와 저장 대안 우선·중복 방지·하루 상한과
// 음식 바꾸기의 권한·균형·알레르기 검사를 검증합니다.
package com.fitplate.fitplateapi.mealplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.ai.FoodAlternativeContext;
import com.fitplate.fitplateapi.ai.MealPlanAiClient;
import com.fitplate.fitplateapi.ai.MealRegenerationContext;
import com.fitplate.fitplateapi.aiusage.domain.AiFeature;
import com.fitplate.fitplateapi.aiusage.service.AiUsageQuota;
import com.fitplate.fitplateapi.exception.AiDailyLimitExceededException;
import com.fitplate.fitplateapi.exception.AllergySafeResultUnavailableException;
import com.fitplate.fitplateapi.mealplan.service.FoodAlternativePool;
import com.fitplate.fitplateapi.favoritefood.domain.FavoriteFood;
import com.fitplate.fitplateapi.favoritefood.repository.FavoriteFoodRepository;
import com.fitplate.fitplateapi.personalization.service.PersonalizationService;
import com.fitplate.fitplateapi.exception.FoodAlternativesUnavailableException;
import com.fitplate.fitplateapi.exception.InvalidRequestException;
import com.fitplate.fitplateapi.exception.InvalidFoodReplacementException;
import com.fitplate.fitplateapi.exception.MealPlanAccessDeniedException;
import com.fitplate.fitplateapi.exception.MealRegenerationUnavailableException;
import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.mealplan.dto.FoodAlternativesResponse;
import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.MealResponse;
import com.fitplate.fitplateapi.mealplan.dto.SavedMealPlanResponse;
import com.fitplate.fitplateapi.mealplan.service.MealPlanEditService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanOwnership;
import com.fitplate.fitplateapi.mealplan.service.MealPlanStorageService;
import com.fitplate.fitplateapi.mealplan.service.MealPlanValidator;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.user.domain.FoodCategory;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static com.fitplate.fitplateapi.mealplan.MealPlanValidatorTests.balancedPlan;
import static com.fitplate.fitplateapi.mealplan.MealPlanValidatorTests.food;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({MealPlanStorageService.class, MealPlanEditService.class, MealPlanValidator.class, MealPlanOwnership.class,
        PersonalizationService.class, FoodAlternativePool.class, AiUsageQuota.class,
        MealPlanEditTests.JsonConfiguration.class})
class MealPlanEditTests {
    private static final String USER_KEY = "edit-test-user";

    @Autowired private MealPlanEditService editService;
    @Autowired private MealPlanStorageService storageService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FavoriteFoodRepository favoriteFoodRepository;

    @MockitoBean private MealPlanAiClient aiClient;
    @MockitoBean private UserProfileService userProfileService;

    private Long planId;

    @BeforeEach
    void prepare() {
        for (String table : List.of("ai_daily_usage", "food_alternative_pool", "food_replacements",
                "meal_plan_foods", "meal_plan_meals", "meal_plans",
                "favorite_foods", "user_profiles", "users")) {
            jdbc.update("DELETE FROM " + table);
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            User user = userRepository.saveAndFlush(User.create(USER_KEY));
            userProfileRepository.saveAndFlush(UserProfile.builder()
                    .user(user).height(175).weight(70).age(30).gender("MALE")
                    .bmi(new BigDecimal("22.86")).build());
        });
        // 점심: [0] 현미밥 400kcal, [1] 연어구이 300kcal
        planId = storageService.saveGeneratedMealPlan(USER_KEY,
                MealPlanRequest.builder().height(175).weight(70).age(30).gender("MALE").goal("MAINTAIN").build(),
                NutritionResult.builder().bmr(1600).tdee(2100).targetCalories(2100)
                        .proteinGram(120).carbsGram(270).fatGram(60).build(),
                balancedPlan("연어구이"));
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(FoodPreferences.none());
    }

    @Test
    void alternativesAreFilteredAndAiIsCalledOutsideTransaction() {
        FoodPreferences noPork = FoodPreferences.of(List.of(), List.of(FoodCategory.PORK));
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(noPork);
        when(aiClient.suggestAlternatives(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return List.of(
                    food("연어 구이", 300, 30, 10, 15),      // 바꿀 음식과 같은 음식
                    food("현미밥", 300, 30, 10, 15),         // 끼니에 이미 있는 음식
                    food("소고기 스테이크", 500, 40, 0, 30), // 칼로리 차이 66%
                    food("제육볶음", 310, 25, 12, 16),       // 제외 분류
                    food("고등어구이", 290, 28, 0, 18),
                    food("고등어 구이", 295, 28, 0, 18),     // 위 후보와 같은 음식
                    food("닭가슴살 스테이크", 280, 35, 5, 8),
                    food("두부부침", 320, 20, 8, 22),
                    food("훈제오리", 310, 22, 2, 24)         // 3개를 넘는 후보
            );
        });

        FoodAlternativesResponse response = editService.suggestAlternatives(USER_KEY, planId, "lunch", 1);

        assertThat(response.getOriginal().getName()).isEqualTo("연어구이");
        assertThat(response.getAlternatives()).extracting(FoodItem::getName)
                .containsExactly("고등어구이", "닭가슴살 스테이크", "두부부침");

        ArgumentCaptor<FoodAlternativeContext> context = ArgumentCaptor.forClass(FoodAlternativeContext.class);
        verify(aiClient).suggestAlternatives(context.capture());
        assertThat(context.getValue().otherFoods()).extracting(FoodItem::getName).containsExactly("현미밥");
        assertThat(context.getValue().preferences()).isEqualTo(noPork);
    }

    @Test
    void favoritesThatFitAreUsedFirstAndAiIsSkippedWhenEnough() {
        saveFavorite("고등어구이", 290);
        saveFavorite("닭가슴살 스테이크", 310);
        saveFavorite("두부부침", 280);

        FoodAlternativesResponse response = editService.suggestAlternatives(USER_KEY, planId, "lunch", 1);

        assertThat(response.getAlternatives()).extracting(FoodItem::getName).hasSize(3);
        assertThat(response.getFavoriteNames()).hasSize(3);
        verify(aiClient, never()).suggestAlternatives(any());
    }

    @Test
    void favoritesFillWhatTheyCanAndAiIsAskedOnlyForTheRest() {
        FoodPreferences noPork = FoodPreferences.of(List.of(), List.of(FoodCategory.PORK));
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(noPork);
        saveFavorite("고등어구이", 290);          // 사용
        saveFavorite("치즈케이크", 600);          // 칼로리 차이가 커서 제외
        saveFavorite("돼지 안심 구이", 300);      // 제외 분류
        when(aiClient.suggestAlternatives(any())).thenReturn(List.of(
                food("고등어 구이", 300, 28, 0, 18),   // 즐겨찾기 대안과 같은 음식
                food("닭가슴살 스테이크", 280, 35, 5, 8),
                food("두부부침", 320, 20, 8, 22)));

        FoodAlternativesResponse response = editService.suggestAlternatives(USER_KEY, planId, "lunch", 1);

        assertThat(response.getAlternatives()).extracting(FoodItem::getName)
                .containsExactly("고등어구이", "닭가슴살 스테이크", "두부부침");
        assertThat(response.getFavoriteNames()).containsExactly("고등어구이");
        ArgumentCaptor<FoodAlternativeContext> context = ArgumentCaptor.forClass(FoodAlternativeContext.class);
        verify(aiClient).suggestAlternatives(context.capture());
        assertThat(context.getValue().count()).isEqualTo(6);
        assertThat(context.getValue().avoidNames()).contains("고등어구이");
    }

    @Test
    void favoriteAlternativesSurviveAiFailure() {
        saveFavorite("고등어구이", 290);
        when(aiClient.suggestAlternatives(any())).thenThrow(new RuntimeException("AI 서버 오류"));

        FoodAlternativesResponse response = editService.suggestAlternatives(USER_KEY, planId, "lunch", 1);

        assertThat(response.getAlternatives()).extracting(FoodItem::getName).containsExactly("고등어구이");
    }

    @Test
    void allergyAlternativesAreDropped() {
        FoodPreferences fishAllergy = FoodPreferences.of(List.of(), List.of(), List.of(FoodCategory.SEAFOOD));
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(fishAllergy);
        saveFavorite("고등어구이", 290);
        when(aiClient.suggestAlternatives(any())).thenReturn(List.of(
                food("새우볶음", 300, 25, 10, 15),
                food("닭가슴살 스테이크", 280, 35, 5, 8)));

        FoodAlternativesResponse response = editService.suggestAlternatives(USER_KEY, planId, "lunch", 1);

        assertThat(response.getAlternatives()).extracting(FoodItem::getName).containsExactly("닭가슴살 스테이크");
        assertThat(response.getFavoriteNames()).isEmpty();
    }

    @Test
    void replacementIsRecordedAndShapesLaterHints() {
        editService.replaceFood(USER_KEY, planId, "lunch", 1, food("고등어구이", 290, 28, 0, 18));
        when(aiClient.regenerateMeal(any())).thenReturn(MealResponse.builder().mealType("dinner").title("저녁").foods(List.of(
                food("고구마", 400, 10, 80, 5), food("두부조림", 300, 30, 10, 15))).build());

        editService.regenerateMeal(USER_KEY, planId, "dinner");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM food_replacements WHERE from_name = '연어구이' AND to_name = '고등어구이'",
                Integer.class)).isEqualTo(1);
        ArgumentCaptor<MealRegenerationContext> context = ArgumentCaptor.forClass(MealRegenerationContext.class);
        verify(aiClient).regenerateMeal(context.capture());
        assertThat(context.getValue().hints().swappedOutFoods()).containsExactly("연어구이");
        assertThat(context.getValue().hints().swappedInFoods()).containsExactly("고등어구이");
    }

    @Test
    void allergyReplacementIsRejected() {
        FoodPreferences fishAllergy = FoodPreferences.of(List.of(), List.of(), List.of(FoodCategory.SEAFOOD));
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(fishAllergy);

        assertThatThrownBy(() -> editService.replaceFood(USER_KEY, planId, "lunch", 0, food("새우볶음밥", 400, 15, 70, 8)))
                .isInstanceOf(InvalidFoodReplacementException.class)
                .hasMessageContaining("알레르기");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM food_replacements", Integer.class)).isZero();
    }

    @Test
    void regenerateMealFailsWhenBothResultsContainAllergyFood() {
        FoodPreferences porkAllergy = FoodPreferences.of(List.of(), List.of(), List.of(FoodCategory.PORK));
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(porkAllergy);
        when(aiClient.regenerateMeal(any())).thenReturn(MealResponse.builder().mealType("lunch").title("점심").foods(List.of(
                food("현미밥", 400, 10, 80, 5), food("제육볶음", 300, 30, 10, 15))).build());

        assertThatThrownBy(() -> editService.regenerateMeal(USER_KEY, planId, "lunch"))
                .isInstanceOf(AllergySafeResultUnavailableException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meal_plan_foods WHERE name = '연어구이'", Integer.class)).isEqualTo(1);
    }

    @Test
    void extraAiAlternativesAreStoredAndServedOnNextPageWithoutAi() {
        when(aiClient.suggestAlternatives(any())).thenReturn(sixAlternatives());

        FoodAlternativesResponse first = editService.suggestAlternatives(USER_KEY, planId, "lunch", 1, List.of());
        List<String> firstNames = first.getAlternatives().stream().map(FoodItem::getName).toList();
        FoodAlternativesResponse second = editService.suggestAlternatives(USER_KEY, planId, "lunch", 1, firstNames);

        assertThat(firstNames).containsExactly("고등어구이", "닭가슴살 스테이크", "두부부침");
        assertThat(second.getAlternatives()).extracting(FoodItem::getName)
                .containsExactly("훈제오리", "소고기 안심", "새우볶음");
        verify(aiClient, times(1)).suggestAlternatives(any());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM food_alternative_pool", Integer.class)).isEqualTo(6);
        assertThat(usedToday(AiFeature.ALTERNATIVES)).isEqualTo(1);
    }

    @Test
    void storedAlternativesAreSharedButFilteredByEachUsersPreferences() {
        when(aiClient.suggestAlternatives(any())).thenReturn(sixAlternatives());
        editService.suggestAlternatives(USER_KEY, planId, "lunch", 1, List.of());

        // 다른 사용자가 같은 음식(연어구이)을 바꾼다. 해산물 알레르기라 새우볶음은 빠진다
        String otherKey = "pool-other-user";
        Long otherPlan = createPlanFor(otherKey);
        when(userProfileService.getFoodPreferences(otherKey))
                .thenReturn(FoodPreferences.of(List.of(), List.of(), List.of(FoodCategory.SEAFOOD)));

        FoodAlternativesResponse response = editService.suggestAlternatives(otherKey, otherPlan, "lunch", 1,
                List.of("닭가슴살 스테이크", "두부부침", "훈제오리"));

        assertThat(response.getAlternatives()).extracting(FoodItem::getName).containsExactly("소고기 안심");
        // 저장분이 모자라 AI 를 다시 불렀지만(재요청 포함 2번) 이 사용자에게 맞는 새 후보는 없었다
        verify(aiClient, times(3)).suggestAlternatives(any());
    }

    @Test
    void shownFoodsAreNeverSuggestedAgainEvenIfAiRepeatsThem() {
        when(aiClient.suggestAlternatives(any()))
                .thenReturn(sixAlternatives())
                .thenReturn(List.of(
                        food("고등어 구이", 300, 28, 0, 18),     // 이미 보여준 음식
                        food("훈제오리", 310, 22, 2, 24),         // 이미 보여준 음식
                        food("참치 스테이크", 295, 33, 1, 14)));
        List<String> shown = List.of("고등어구이", "닭가슴살 스테이크", "두부부침", "훈제오리", "소고기 안심", "새우볶음");
        editService.suggestAlternatives(USER_KEY, planId, "lunch", 1, List.of());

        FoodAlternativesResponse response = editService.suggestAlternatives(USER_KEY, planId, "lunch", 1, shown);

        assertThat(response.getAlternatives()).extracting(FoodItem::getName).containsExactly("참치 스테이크");
        ArgumentCaptor<FoodAlternativeContext> context = ArgumentCaptor.forClass(FoodAlternativeContext.class);
        verify(aiClient, times(2)).suggestAlternatives(context.capture());
        assertThat(context.getAllValues().get(1).avoidNames()).containsAll(shown);
    }

    @Test
    void whenNothingNewIsLeftTheUserIsTold() {
        when(aiClient.suggestAlternatives(any())).thenReturn(List.of(food("고등어구이", 290, 28, 0, 18)));

        assertThatThrownBy(() -> editService.suggestAlternatives(USER_KEY, planId, "lunch", 1, List.of("고등어구이")))
                .isInstanceOf(FoodAlternativesUnavailableException.class)
                .hasMessageContaining("새로운 음식");
    }

    @Test
    void dailyLimitStopsAiButStoredAlternativesStillWork() {
        setUsedToday(AiFeature.ALTERNATIVES, AiFeature.ALTERNATIVES.dailyLimit());

        assertThatThrownBy(() -> editService.suggestAlternatives(USER_KEY, planId, "lunch", 1, List.of()))
                .isInstanceOf(AiDailyLimitExceededException.class);
        verify(aiClient, never()).suggestAlternatives(any());

        saveFavorite("고등어구이", 290);
        FoodAlternativesResponse response = editService.suggestAlternatives(USER_KEY, planId, "lunch", 1, List.of());
        assertThat(response.getAlternatives()).extracting(FoodItem::getName).containsExactly("고등어구이");
        verify(aiClient, never()).suggestAlternatives(any());
    }

    @Test
    void failedAiCallDoesNotUseUpTheDailyLimit() {
        when(aiClient.suggestAlternatives(any())).thenThrow(new RuntimeException("AI 서버 오류"));

        assertThatThrownBy(() -> editService.suggestAlternatives(USER_KEY, planId, "lunch", 1, List.of()))
                .isInstanceOf(RuntimeException.class);
        assertThat(usedToday(AiFeature.ALTERNATIVES)).isZero();
    }

    @Test
    void mealRegenerationHasItsOwnDailyLimit() {
        setUsedToday(AiFeature.MEAL_REGENERATION, AiFeature.MEAL_REGENERATION.dailyLimit());

        assertThatThrownBy(() -> editService.regenerateMeal(USER_KEY, planId, "lunch"))
                .isInstanceOf(AiDailyLimitExceededException.class);
        verify(aiClient, never()).regenerateMeal(any());
    }

    @Test
    void planGetsAutomaticTitleThatFollowsFoodChanges() {
        assertThat(editService.renamePlan(USER_KEY, planId, null).getTitle()).isEqualTo("연어구이·두부 유지 식단");

        SavedMealPlanResponse replaced = editService.replaceFood(USER_KEY, planId, "lunch", 1, food("고등어구이", 290, 28, 0, 18));

        assertThat(replaced.getTitle()).isEqualTo("고등어구이·두부 유지 식단");
        assertThat(replaced.isTitleCustomized()).isFalse();
    }

    @Test
    void userCanRenameAndResetTitle() {
        SavedMealPlanResponse renamed = editService.renamePlan(USER_KEY, planId, "  주말 다이어트  ");
        assertThat(renamed.getTitle()).isEqualTo("주말 다이어트");
        assertThat(renamed.isTitleCustomized()).isTrue();
        assertThat(jdbc.queryForObject("SELECT title FROM meal_plans", String.class)).isEqualTo("주말 다이어트");

        SavedMealPlanResponse reset = editService.renamePlan(USER_KEY, planId, " ");
        assertThat(reset.getTitle()).isEqualTo("연어구이·두부 유지 식단");
        assertThat(reset.isTitleCustomized()).isFalse();

        assertThatThrownBy(() -> editService.renamePlan(USER_KEY, planId, "가".repeat(31)))
                .isInstanceOf(InvalidRequestException.class);
        userRepository.saveAndFlush(User.create("rename-other-user"));
        assertThatThrownBy(() -> editService.renamePlan("rename-other-user", planId, "남의 식단"))
                .isInstanceOf(MealPlanAccessDeniedException.class);
    }

    @Test
    void noUsableAlternativeRetriesOnceThenFails() {
        when(aiClient.suggestAlternatives(any())).thenReturn(List.of(food("소고기 스테이크", 900, 40, 0, 60)));

        assertThatThrownBy(() -> editService.suggestAlternatives(USER_KEY, planId, "lunch", 1))
                .isInstanceOf(FoodAlternativesUnavailableException.class);

        ArgumentCaptor<FoodAlternativeContext> context = ArgumentCaptor.forClass(FoodAlternativeContext.class);
        verify(aiClient, times(2)).suggestAlternatives(context.capture());
        assertThat(context.getAllValues().get(1).avoidNames()).containsExactly("소고기 스테이크");
    }

    @Test
    void replaceFoodUpdatesOnlyThatFood() {
        SavedMealPlanResponse response = editService.replaceFood(USER_KEY, planId, "lunch", 1,
                food("고등어구이", 290, 28, 0, 18));

        List<String> lunch = response.getAiMealPlanResponse().getMeals().stream()
                .filter(meal -> meal.getMealType().equals("lunch"))
                .findFirst().orElseThrow()
                .getFoods().stream().map(FoodItem::getName).toList();
        assertThat(lunch).containsExactly("현미밥", "고등어구이");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meal_plan_foods WHERE name = '연어구이'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meal_plan_foods", Integer.class)).isEqualTo(6);
    }

    @Test
    void replacementThatBreaksCalorieBalanceIsRejected() {
        assertThatThrownBy(() -> editService.replaceFood(USER_KEY, planId, "lunch", 1,
                food("삼겹살", 700, 30, 0, 60)))
                .isInstanceOf(InvalidFoodReplacementException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meal_plan_foods WHERE name = '연어구이'", Integer.class)).isEqualTo(1);
    }

    @Test
    void otherUsersPlanAndMissingFoodAreRejected() {
        userRepository.saveAndFlush(User.create("other-user"));

        assertThatThrownBy(() -> editService.replaceFood("other-user", planId, "lunch", 1, food("고등어구이", 290, 28, 0, 18)))
                .isInstanceOf(MealPlanAccessDeniedException.class);
        assertThatThrownBy(() -> editService.suggestAlternatives("other-user", planId, "lunch", 1))
                .isInstanceOf(MealPlanAccessDeniedException.class);
        assertThatThrownBy(() -> editService.replaceFood(USER_KEY, planId, "lunch", 5, food("고등어구이", 290, 28, 0, 18)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void regenerateMealReplacesOnlyThatMealAndCallsAiOutsideTransaction() {
        when(aiClient.regenerateMeal(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return MealResponse.builder().mealType("lunch").title("새 점심").foods(List.of(
                    food("잡곡밥", 380, 9, 78, 4),
                    food("고등어구이", 320, 30, 0, 17),
                    food("시금치나물", 30, 2, 5, 1)
            )).build();
        });

        SavedMealPlanResponse response = editService.regenerateMeal(USER_KEY, planId, "lunch");

        ArgumentCaptor<MealRegenerationContext> context = ArgumentCaptor.forClass(MealRegenerationContext.class);
        verify(aiClient).regenerateMeal(context.capture());
        assertThat(context.getValue().target().calories()).isEqualTo(700);
        assertThat(context.getValue().currentFoods()).extracting(FoodItem::getName).containsExactly("현미밥", "연어구이");
        assertThat(context.getValue().otherMealFoods()).hasSize(4);

        MealResponse lunch = response.getAiMealPlanResponse().getMeals().stream()
                .filter(meal -> meal.getMealType().equals("lunch")).findFirst().orElseThrow();
        assertThat(lunch.getTitle()).isEqualTo("새 점심");
        assertThat(lunch.getFoods()).extracting(FoodItem::getName).containsExactly("잡곡밥", "고등어구이", "시금치나물");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meal_plan_foods", Integer.class)).isEqualTo(7);
    }

    @Test
    void regenerateMealRetriesWhenExcludedFoodAppears() {
        FoodPreferences noPork = FoodPreferences.of(List.of(), List.of(FoodCategory.PORK));
        when(userProfileService.getFoodPreferences(USER_KEY)).thenReturn(noPork);
        when(aiClient.regenerateMeal(any()))
                .thenReturn(MealResponse.builder().mealType("lunch").title("점심").foods(List.of(
                        food("현미밥", 400, 10, 80, 5), food("제육볶음", 300, 30, 10, 15))).build())
                .thenReturn(MealResponse.builder().mealType("lunch").title("점심").foods(List.of(
                        food("현미밥", 400, 10, 80, 5), food("닭가슴살 구이", 300, 30, 10, 15))).build());

        SavedMealPlanResponse response = editService.regenerateMeal(USER_KEY, planId, "lunch");

        ArgumentCaptor<MealRegenerationContext> context = ArgumentCaptor.forClass(MealRegenerationContext.class);
        verify(aiClient, times(2)).regenerateMeal(context.capture());
        assertThat(context.getAllValues().get(1).feedback()).contains("제육볶음");
        assertThat(response.getAiMealPlanResponse().getMeals().stream()
                .flatMap(meal -> meal.getFoods().stream()).map(FoodItem::getName))
                .contains("닭가슴살 구이").doesNotContain("제육볶음");
    }

    @Test
    void regenerateMealFailsWithoutChangesWhenAiKeepsReturningWrongShape() {
        when(aiClient.regenerateMeal(any())).thenReturn(MealResponse.builder()
                .mealType("dinner").title("저녁").foods(List.of(food("현미밥", 400, 10, 80, 5))).build());

        assertThatThrownBy(() -> editService.regenerateMeal(USER_KEY, planId, "lunch"))
                .isInstanceOf(MealRegenerationUnavailableException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meal_plan_foods WHERE name = '연어구이'", Integer.class)).isEqualTo(1);
    }

    @Test
    void otherUserCannotRegenerateMeal() {
        userRepository.saveAndFlush(User.create("other-user"));

        assertThatThrownBy(() -> editService.regenerateMeal("other-user", planId, "lunch"))
                .isInstanceOf(MealPlanAccessDeniedException.class);
    }

    private static List<FoodItem> sixAlternatives() {
        return List.of(
                food("고등어구이", 290, 28, 0, 18),
                food("닭가슴살 스테이크", 280, 35, 5, 8),
                food("두부부침", 320, 20, 8, 22),
                food("훈제오리", 310, 22, 2, 24),
                food("소고기 안심", 305, 30, 0, 19),
                food("새우볶음", 300, 25, 10, 15));
    }

    private Long createPlanFor(String userKey) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            User user = userRepository.saveAndFlush(User.create(userKey));
            userProfileRepository.saveAndFlush(UserProfile.builder()
                    .user(user).height(165).weight(55).age(28).gender("FEMALE")
                    .bmi(new BigDecimal("20.20")).build());
        });
        return storageService.saveGeneratedMealPlan(userKey,
                MealPlanRequest.builder().height(165).weight(55).age(28).gender("FEMALE").goal("MAINTAIN").build(),
                NutritionResult.builder().bmr(1300).tdee(1800).targetCalories(1800)
                        .proteinGram(100).carbsGram(230).fatGram(50).build(),
                balancedPlan("연어구이"));
    }

    private int usedToday(AiFeature feature) {
        List<Integer> counts = jdbc.queryForList(
                "SELECT used_count FROM ai_daily_usage WHERE feature = ? AND usage_date = ?",
                Integer.class, feature.name(), LocalDate.now(ZoneId.of("Asia/Seoul")));
        return counts.isEmpty() ? 0 : counts.get(0);
    }

    private void setUsedToday(AiFeature feature, int count) {
        Long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE toss_user_key = ?", Long.class, USER_KEY);
        jdbc.update("INSERT INTO ai_daily_usage (user_id, usage_date, feature, used_count) VALUES (?, ?, ?, ?)",
                userId, LocalDate.now(ZoneId.of("Asia/Seoul")), feature.name(), count);
    }

    private void saveFavorite(String name, int calories) {
        User user = userRepository.findByTossUserKey(USER_KEY).orElseThrow();
        favoriteFoodRepository.saveAndFlush(FavoriteFood.builder()
                .user(user).foodName(name).amount("1인분").calories(calories)
                .protein(new BigDecimal("25")).carbohydrate(new BigDecimal("8")).fat(new BigDecimal("12"))
                .shoppingKeyword(name).build());
    }

    @TestConfiguration
    static class JsonConfiguration {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        Clock clock() {
            return Clock.system(ZoneId.of("Asia/Seoul"));
        }
    }
}
