// 최근 식단·즐겨찾기·교체 이력으로 만드는 개인화 힌트의 집계 규칙을 검증합니다.
package com.fitplate.fitplateapi.personalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.ai.PersonalizationHints;
import com.fitplate.fitplateapi.favoritefood.domain.FavoriteFood;
import com.fitplate.fitplateapi.favoritefood.repository.FavoriteFoodRepository;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.service.MealPlanStorageService;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.personalization.service.PersonalizationService;
import com.fitplate.fitplateapi.user.domain.FoodCategory;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.fitplate.fitplateapi.mealplan.MealPlanValidatorTests.balancedPlan;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({PersonalizationService.class, MealPlanStorageService.class, PersonalizationServiceTests.TestBeans.class})
class PersonalizationServiceTests {
    private static final String USER_KEY = "hint-test-user";

    @Autowired private PersonalizationService personalizationService;
    @Autowired private MealPlanStorageService storageService;
    @Autowired private UserRepository userRepository;
    @Autowired private FavoriteFoodRepository favoriteFoodRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private User user;

    @BeforeEach
    void prepare() {
        for (String table : List.of("food_replacements", "meal_plan_foods", "meal_plan_meals", "meal_plans",
                "favorite_foods", "user_profiles", "users")) {
            jdbc.update("DELETE FROM " + table);
        }
        user = new TransactionTemplate(transactionManager).execute(status -> {
            User created = userRepository.saveAndFlush(User.create(USER_KEY));
            userProfileRepository.saveAndFlush(UserProfile.builder()
                    .user(created).height(175).weight(70).age(30).gender("MALE")
                    .bmi(new BigDecimal("22.86")).build());
            return created;
        });
    }

    @Test
    void foodsRepeatedInRecentPlansBecomeRecentHints() {
        // 현미밥은 4번, 닭가슴살·두부·고구마는 2번, 연어구이·고등어구이는 1번씩 나온다
        saveGeneratedPlan("연어구이");
        saveGeneratedPlan("고등어구이");

        PersonalizationHints hints = personalizationService.hintsFor(USER_KEY, FoodPreferences.none());

        assertThat(hints.recentFoods()).first().isEqualTo("현미밥");
        assertThat(hints.recentFoods()).contains("닭가슴살", "두부", "고구마", "현미밥")
                .doesNotContain("고등어구이");
    }

    @Test
    void oldPlansAreIgnored() {
        saveGeneratedPlan("연어구이");
        jdbc.update("UPDATE meal_plans SET created_at = ?", LocalDateTime.now().minusDays(30));

        assertThat(personalizationService.hintsFor(USER_KEY, FoodPreferences.none()).recentFoods()).isEmpty();
    }

    @Test
    void favoritesSkipExcludedAllergyAndRecentFoods() {
        saveGeneratedPlan("연어구이");
        saveFavorite("그릭요거트");
        saveFavorite("땅콩버터");     // 알레르기
        saveFavorite("제육볶음");     // 제외
        saveFavorite("현미밥");       // 최근 자주 나온 음식

        FoodPreferences preferences = FoodPreferences.of(List.of(), List.of(FoodCategory.PORK), List.of(FoodCategory.NUTS));
        PersonalizationHints hints = personalizationService.hintsFor(USER_KEY, preferences);

        assertThat(hints.favoriteFoods()).containsExactly("그릭요거트");
    }

    @Test
    void latestSwapWinsWhenFoodWasSwappedBothWays() {
        personalizationService.recordReplacement(user, "연어구이", "고등어구이");
        jdbc.update("UPDATE food_replacements SET created_at = ?", LocalDateTime.now().minusDays(2));
        personalizationService.recordReplacement(user, "고등어구이", "닭가슴살 스테이크");
        personalizationService.recordReplacement(user, "현미밥", "현미 밥");   // 같은 음식은 남기지 않음

        PersonalizationHints hints = personalizationService.hintsFor(USER_KEY, FoodPreferences.none());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM food_replacements", Integer.class)).isEqualTo(2);
        assertThat(hints.swappedInFoods()).containsExactly("닭가슴살 스테이크");
        assertThat(hints.swappedOutFoods()).containsExactly("고등어구이", "연어구이");
    }

    @Test
    void unknownUserGetsEmptyHints() {
        assertThat(personalizationService.hintsFor("nobody", FoodPreferences.none()).isEmpty()).isTrue();
        assertThat(personalizationService.favoriteCandidates("nobody")).isEmpty();
    }

    private void saveGeneratedPlan(String lunchMain) {
        storageService.saveGeneratedMealPlan(USER_KEY,
                MealPlanRequest.builder().height(175).weight(70).age(30).gender("MALE").goal("MAINTAIN").build(),
                NutritionResult.builder().bmr(1600).tdee(2100).targetCalories(2100)
                        .proteinGram(120).carbsGram(270).fatGram(60).build(),
                balancedPlan(lunchMain));
    }

    private void saveFavorite(String name) {
        favoriteFoodRepository.saveAndFlush(FavoriteFood.builder()
                .user(user).foodName(name).amount("1인분").calories(200)
                .protein(new BigDecimal("10")).carbohydrate(new BigDecimal("10")).fat(new BigDecimal("10"))
                .shoppingKeyword(name).build());
    }

    @TestConfiguration
    static class TestBeans {
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
