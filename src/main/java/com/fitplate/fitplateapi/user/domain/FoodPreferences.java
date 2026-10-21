// 식단 생성에 반영할 선호·제외·알레르기 음식 분류입니다.
// 같은 분류가 여러 곳에 있으면 알레르기 > 제외 > 선호 순으로 이깁니다.
package com.fitplate.fitplateapi.user.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record FoodPreferences(Set<FoodCategory> preferred, Set<FoodCategory> excluded, Set<FoodCategory> allergies) {

    public FoodPreferences {
        EnumSet<FoodCategory> allergyOnly = copy(allergies);
        EnumSet<FoodCategory> excludedOnly = copy(excluded);
        excludedOnly.removeAll(allergyOnly);
        EnumSet<FoodCategory> preferredOnly = copy(preferred);
        preferredOnly.removeAll(allergyOnly);
        preferredOnly.removeAll(excludedOnly);
        allergies = Collections.unmodifiableSet(allergyOnly);
        excluded = Collections.unmodifiableSet(excludedOnly);
        preferred = Collections.unmodifiableSet(preferredOnly);
    }

    public static FoodPreferences none() {
        return new FoodPreferences(Set.of(), Set.of(), Set.of());
    }

    public static FoodPreferences of(Collection<FoodCategory> preferred, Collection<FoodCategory> excluded) {
        return of(preferred, excluded, Set.of());
    }

    public static FoodPreferences of(Collection<FoodCategory> preferred, Collection<FoodCategory> excluded,
                                     Collection<FoodCategory> allergies) {
        return new FoodPreferences(copy(preferred), copy(excluded), copy(allergies));
    }

    /** 음식 이름·키워드가 제외 또는 알레르기 분류에 걸리면 그 분류를, 아니면 null 을 돌려준다. */
    public FoodCategory findExcludedMatch(String... texts) {
        FoodCategory allergy = findAllergyMatch(texts);
        return allergy != null ? allergy : findIn(excluded, texts);
    }

    /** 알레르기 분류에 걸리면 그 분류를, 아니면 null. 키워드 판별이라 모든 성분을 잡지는 못한다. */
    public FoodCategory findAllergyMatch(String... texts) {
        return findIn(allergies, texts);
    }

    private static FoodCategory findIn(Set<FoodCategory> categories, String... texts) {
        for (FoodCategory category : categories) {
            for (String text : texts) {
                if (category.matches(text)) {
                    return category;
                }
            }
        }
        return null;
    }

    private static EnumSet<FoodCategory> copy(Collection<FoodCategory> categories) {
        EnumSet<FoodCategory> result = EnumSet.noneOf(FoodCategory.class);
        if (categories != null) {
            categories.stream().filter(Objects::nonNull).forEach(result::add);
        }
        return result;
    }
}
