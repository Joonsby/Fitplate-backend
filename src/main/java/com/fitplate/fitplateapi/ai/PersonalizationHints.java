// DB 에 쌓인 사용 기록으로 만든 추천 참고 정보입니다. 강제 조건이 아니라 프롬프트 힌트로만 씁니다.
package com.fitplate.fitplateapi.ai;

import java.util.List;

public record PersonalizationHints(
        List<String> recentFoods,
        List<String> favoriteFoods,
        List<String> swappedOutFoods,
        List<String> swappedInFoods
) {
    public PersonalizationHints {
        recentFoods = List.copyOf(recentFoods);
        favoriteFoods = List.copyOf(favoriteFoods);
        swappedOutFoods = List.copyOf(swappedOutFoods);
        swappedInFoods = List.copyOf(swappedInFoods);
    }

    public static PersonalizationHints none() {
        return new PersonalizationHints(List.of(), List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return recentFoods.isEmpty() && favoriteFoods.isEmpty()
                && swappedOutFoods.isEmpty() && swappedInFoods.isEmpty();
    }
}
