// 내 프로필 저장과 식단 생성 요청에서 선호·제외 음식 분류가 저장·유지되는지 검증합니다.
package com.fitplate.fitplateapi.user;

import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.nutrition.service.NutritionCalculator;
import com.fitplate.fitplateapi.user.domain.FoodCategory;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.dto.UserProfileResponse;
import com.fitplate.fitplateapi.user.dto.UserProfileUpdateRequest;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import com.fitplate.fitplateapi.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({UserProfileService.class, NutritionCalculator.class})
class UserProfilePreferenceTests {
    private static final String USER_KEY = "preference-test-user";

    @Autowired private UserProfileService userProfileService;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        for (String table : List.of("meal_plan_foods", "meal_plan_meals", "meal_plans",
                "favorite_foods", "user_profiles", "users")) {
            jdbc.update("DELETE FROM " + table);
        }
        userRepository.saveAndFlush(User.create(USER_KEY));
    }

    @Test
    void updateCreatesProfileAndStoresPreferences() {
        UserProfileResponse saved = userProfileService.updateMyProfile(USER_KEY, update(
                List.of(FoodCategory.CHICKEN, FoodCategory.SEAFOOD), List.of(FoodCategory.PORK)));

        assertThat(saved.getHeight()).isEqualTo(172);
        assertThat(saved.getPreferredFoodCategories()).containsExactly(FoodCategory.CHICKEN, FoodCategory.SEAFOOD);
        assertThat(saved.getExcludedFoodCategories()).containsExactly(FoodCategory.PORK);
        assertThat(jdbc.queryForObject("SELECT excluded_food_categories FROM user_profiles", String.class))
                .isEqualTo("PORK");
        assertThat(userProfileService.getMyProfile(USER_KEY).orElseThrow().getPreferredFoodCategories())
                .containsExactly(FoodCategory.CHICKEN, FoodCategory.SEAFOOD);
    }

    @Test
    void sameCategoryInBothListsIsStoredAsExcluded() {
        UserProfileResponse saved = userProfileService.updateMyProfile(USER_KEY, update(
                List.of(FoodCategory.EGG, FoodCategory.NUTS), List.of(FoodCategory.NUTS)));

        assertThat(saved.getPreferredFoodCategories()).containsExactly(FoodCategory.EGG);
        assertThat(saved.getExcludedFoodCategories()).containsExactly(FoodCategory.NUTS);
    }

    @Test
    void mealPlanRequestWithoutPreferencesKeepsStoredOnesAndEmptyListClears() {
        userProfileService.updateMyProfile(USER_KEY, update(List.of(FoodCategory.BEEF), List.of(FoodCategory.WHEAT)));

        userProfileService.upsertFromMealPlanRequest(USER_KEY, MealPlanRequest.builder()
                .height(173).weight(70).age(31).gender("MALE").goal("MAINTAIN").build());

        assertThat(userProfileService.getFoodPreferences(USER_KEY).excluded()).containsExactly(FoodCategory.WHEAT);
        assertThat(userProfileService.getMyProfile(USER_KEY).orElseThrow().getHeight()).isEqualTo(173);

        userProfileService.updateMyProfile(USER_KEY, update(null, List.of()));

        assertThat(userProfileService.getFoodPreferences(USER_KEY).excluded()).isEmpty();
        assertThat(userProfileService.getFoodPreferences(USER_KEY).preferred()).containsExactly(FoodCategory.BEEF);
    }

    @Test
    void allergiesAreStoredSeparatelyAndKeptWhenOmitted() {
        UserProfileResponse saved = userProfileService.updateMyProfile(USER_KEY, update(
                List.of(FoodCategory.NUTS, FoodCategory.EGG), List.of(FoodCategory.PORK), List.of(FoodCategory.NUTS)));

        assertThat(saved.getAllergyFoodCategories()).containsExactly(FoodCategory.NUTS);
        assertThat(saved.getPreferredFoodCategories()).containsExactly(FoodCategory.EGG);
        assertThat(jdbc.queryForObject("SELECT allergy_food_categories FROM user_profiles", String.class)).isEqualTo("NUTS");

        // 알레르기를 보내지 않으면(null) 그대로 두고, 빈 배열이면 해제한다
        userProfileService.upsertFromMealPlanRequest(USER_KEY, MealPlanRequest.builder()
                .height(173).weight(70).age(31).gender("MALE").goal("MAINTAIN").build());
        assertThat(userProfileService.getFoodPreferences(USER_KEY).allergies()).containsExactly(FoodCategory.NUTS);

        userProfileService.updateMyProfile(USER_KEY, update(null, null, List.of()));
        assertThat(userProfileService.getFoodPreferences(USER_KEY).allergies()).isEmpty();
        assertThat(userProfileService.getFoodPreferences(USER_KEY).excluded()).containsExactly(FoodCategory.PORK);
    }

    @Test
    void userWithoutProfileHasNoPreferences() {
        assertThat(userProfileService.getFoodPreferences(USER_KEY).excluded()).isEmpty();
        assertThat(userProfileService.getMyProfile(USER_KEY)).isEmpty();
    }

    private UserProfileUpdateRequest update(List<FoodCategory> preferred, List<FoodCategory> excluded) {
        return update(preferred, excluded, null);
    }

    private UserProfileUpdateRequest update(List<FoodCategory> preferred, List<FoodCategory> excluded,
                                            List<FoodCategory> allergies) {
        return UserProfileUpdateRequest.builder()
                .height(172).weight(68).age(30).gender("MALE")
                .preferredFoodCategories(preferred)
                .excludedFoodCategories(excluded)
                .allergyFoodCategories(allergies)
                .build();
    }
}
