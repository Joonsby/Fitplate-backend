// 음식 분류 키워드 판별과 선호·제외 정리, 컬럼 변환을 검증합니다.
package com.fitplate.fitplateapi.user;

import com.fitplate.fitplateapi.user.domain.FoodCategory;
import com.fitplate.fitplateapi.user.domain.FoodCategorySetConverter;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FoodPreferencesTests {

    @Test
    void categoryKeywordsMatchTypicalFoodNamesIgnoringSpaces() {
        assertThat(FoodCategory.PORK.matches("삼겹살 구이")).isTrue();
        assertThat(FoodCategory.CHICKEN.matches("닭 가슴살")).isTrue();
        assertThat(FoodCategory.SEAFOOD.matches("연어 스테이크")).isTrue();
        assertThat(FoodCategory.NUTS.matches("아몬드")).isTrue();
        assertThat(FoodCategory.EGG.matches("계란후라이")).isTrue();
    }

    @Test
    void categoryKeywordsAvoidCommonFalsePositives() {
        assertThat(FoodCategory.BEEF.matches("연어 스테이크")).isFalse();
        assertThat(FoodCategory.WHEAT.matches("오트밀")).isFalse();
        assertThat(FoodCategory.CHICKEN.matches("달걀")).isFalse();
        assertThat(FoodCategory.PORK.matches("현미밥")).isFalse();
    }

    @Test
    void excludedWinsWhenCategoryIsInBothLists() {
        FoodPreferences preferences = FoodPreferences.of(
                List.of(FoodCategory.CHICKEN, FoodCategory.PORK),
                List.of(FoodCategory.PORK));

        assertThat(preferences.preferred()).containsExactly(FoodCategory.CHICKEN);
        assertThat(preferences.excluded()).containsExactly(FoodCategory.PORK);
        assertThat(preferences.findExcludedMatch("제육볶음", "제육")).isEqualTo(FoodCategory.PORK);
        assertThat(preferences.findExcludedMatch("닭가슴살", "닭가슴살")).isNull();
    }

    @Test
    void allergyWinsOverExcludedAndPreferred() {
        FoodPreferences preferences = FoodPreferences.of(
                List.of(FoodCategory.SEAFOOD, FoodCategory.EGG),
                List.of(FoodCategory.SEAFOOD, FoodCategory.PORK),
                List.of(FoodCategory.SEAFOOD));

        assertThat(preferences.allergies()).containsExactly(FoodCategory.SEAFOOD);
        assertThat(preferences.excluded()).containsExactly(FoodCategory.PORK);
        assertThat(preferences.preferred()).containsExactly(FoodCategory.EGG);
        assertThat(preferences.findAllergyMatch("새우볶음밥")).isEqualTo(FoodCategory.SEAFOOD);
        assertThat(preferences.findAllergyMatch("제육볶음")).isNull();
        // 제외 판별은 알레르기도 함께 잡는다
        assertThat(preferences.findExcludedMatch("새우볶음밥")).isEqualTo(FoodCategory.SEAFOOD);
        assertThat(preferences.findExcludedMatch("제육볶음")).isEqualTo(FoodCategory.PORK);
    }

    @Test
    void converterRoundTripsAndTreatsEmptyAsNull() {
        FoodCategorySetConverter converter = new FoodCategorySetConverter();

        String column = converter.convertToDatabaseColumn(EnumSet.of(FoodCategory.NUTS, FoodCategory.PORK));

        assertThat(column).isEqualTo("PORK,NUTS");
        assertThat(converter.convertToEntityAttribute(column)).containsExactlyInAnyOrder(FoodCategory.PORK, FoodCategory.NUTS);
        assertThat(converter.convertToDatabaseColumn(Set.of())).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }
}
