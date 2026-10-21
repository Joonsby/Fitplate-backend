// 최근 식단·즐겨찾기·교체 이력처럼 DB 에 이미 있는 기록으로 추천 힌트와 대안 후보를 만듭니다.
package com.fitplate.fitplateapi.personalization.service;

import com.fitplate.fitplateapi.ai.PersonalizationHints;
import com.fitplate.fitplateapi.favoritefood.domain.FavoriteFood;
import com.fitplate.fitplateapi.favoritefood.repository.FavoriteFoodRepository;
import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.repository.MealPlanRepository;
import com.fitplate.fitplateapi.personalization.domain.FoodReplacement;
import com.fitplate.fitplateapi.personalization.repository.FoodReplacementRepository;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PersonalizationService {

    static final int RECENT_DAYS = 14;
    /** 이 횟수 이상 나온 음식만 '최근 자주 나온 음식'으로 본다. */
    static final int RECENT_MIN_USES = 2;
    static final int RECENT_LIMIT = 8;
    static final int FAVORITE_LIMIT = 5;
    static final int SWAP_DAYS = 60;
    static final int SWAP_LIMIT = 5;

    private final UserRepository userRepository;
    private final MealPlanRepository mealPlanRepository;
    private final FavoriteFoodRepository favoriteFoodRepository;
    private final FoodReplacementRepository foodReplacementRepository;
    private final Clock clock;

    /** 제외·알레르기에 걸리는 음식은 힌트에 넣지 않는다. 사용자가 없으면 빈 힌트. */
    @Transactional(readOnly = true)
    public PersonalizationHints hintsFor(String tossUserKey, FoodPreferences preferences) {
        User user = userRepository.findByTossUserKey(tossUserKey).orElse(null);
        if (user == null) {
            return PersonalizationHints.none();
        }
        LocalDateTime now = LocalDateTime.now(clock);

        List<String> recent = new ArrayList<>();
        for (Object[] row : mealPlanRepository.countRecentFoodNames(user, now.minusDays(RECENT_DAYS))) {
            if (recent.size() == RECENT_LIMIT || ((Number) row[1]).longValue() < RECENT_MIN_USES) {
                break;
            }
            recent.add((String) row[0]);
        }
        Set<String> recentKeys = keys(recent);

        List<String> favorites = favoriteFoodRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(FavoriteFood::getFoodName)
                .filter(name -> allowed(name, preferences) && !recentKeys.contains(key(name)))
                .distinct()
                .limit(FAVORITE_LIMIT)
                .toList();

        // 같은 음식을 넣었다 뺐다 했으면 가장 최근 행동만 남긴다
        Set<String> seen = new HashSet<>();
        List<String> swappedOut = new ArrayList<>();
        List<String> swappedIn = new ArrayList<>();
        for (FoodReplacement replacement : foodReplacementRepository
                .findTop50ByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDescIdDesc(user, now.minusDays(SWAP_DAYS))) {
            if (seen.add(key(replacement.getToName())) && swappedIn.size() < SWAP_LIMIT
                    && allowed(replacement.getToName(), preferences) && !recentKeys.contains(key(replacement.getToName()))) {
                swappedIn.add(replacement.getToName());
            }
            if (seen.add(key(replacement.getFromName())) && swappedOut.size() < SWAP_LIMIT) {
                swappedOut.add(replacement.getFromName());
            }
        }
        return new PersonalizationHints(recent, favorites, swappedOut, swappedIn);
    }

    /** 영양 정보가 모두 있는 즐겨찾기 음식을 대안 후보로 돌려준다. 최근 저장한 순서. */
    @Transactional(readOnly = true)
    public List<FoodItem> favoriteCandidates(String tossUserKey) {
        return userRepository.findByTossUserKey(tossUserKey)
                .map(user -> favoriteFoodRepository.findByUserOrderByCreatedAtDesc(user).stream()
                        .filter(food -> food.getCalories() != null && food.getProtein() != null
                                && food.getCarbohydrate() != null && food.getFat() != null
                                && food.getAmount() != null && !food.getAmount().isBlank())
                        .map(food -> FoodItem.builder()
                                .name(food.getFoodName())
                                .amount(food.getAmount())
                                .calories(food.getCalories())
                                .protein(food.getProtein().doubleValue())
                                .carbohydrate(food.getCarbohydrate().doubleValue())
                                .fat(food.getFat().doubleValue())
                                .shoppingKeyword(food.getShoppingKeyword() == null ? food.getFoodName() : food.getShoppingKeyword())
                                .build())
                        .toList())
                .orElse(List.of());
    }

    /** 교체 이력을 남긴다. 같은 이름으로 바꾼 경우는 남기지 않는다. 호출하는 쪽 트랜잭션에 묶인다. */
    @Transactional
    public void recordReplacement(User user, String fromName, String toName) {
        if (key(fromName).equals(key(toName))) {
            return;
        }
        foodReplacementRepository.save(new FoodReplacement(user, fromName, toName, LocalDateTime.now(clock)));
    }

    private static boolean allowed(String name, FoodPreferences preferences) {
        return preferences == null || preferences.findExcludedMatch(name) == null;
    }

    private static Set<String> keys(List<String> names) {
        Set<String> keys = new LinkedHashSet<>();
        names.forEach(name -> keys.add(key(name)));
        return keys;
    }

    private static String key(String name) {
        return name == null ? "" : name.replace(" ", "");
    }
}
