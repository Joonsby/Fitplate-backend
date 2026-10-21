// 저장된 식단의 음식 한 개를 대안으로 바꾸거나 한 끼를 AI 로 다시 구성합니다.
// 대안은 즐겨찾기 → 저장된 대안 → AI 순으로 채우고, AI 호출은 하루 상한 안에서만 합니다.
package com.fitplate.fitplateapi.mealplan.service;

import com.fitplate.fitplateapi.ai.FoodAlternativeContext;
import com.fitplate.fitplateapi.ai.MealPlanAiClient;
import com.fitplate.fitplateapi.ai.MealRegenerationContext;
import com.fitplate.fitplateapi.ai.MealRegenerationContext.MealTarget;
import com.fitplate.fitplateapi.ai.PersonalizationHints;
import com.fitplate.fitplateapi.aiusage.domain.AiFeature;
import com.fitplate.fitplateapi.aiusage.service.AiUsageQuota;
import com.fitplate.fitplateapi.exception.AiDailyLimitExceededException;
import com.fitplate.fitplateapi.exception.AllergySafeResultUnavailableException;
import com.fitplate.fitplateapi.exception.FoodAlternativesUnavailableException;
import com.fitplate.fitplateapi.exception.InvalidFoodReplacementException;
import com.fitplate.fitplateapi.exception.InvalidRequestException;
import com.fitplate.fitplateapi.exception.MealRegenerationUnavailableException;
import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.mealplan.domain.MealPlanFood;
import com.fitplate.fitplateapi.mealplan.domain.MealPlanMeal;
import com.fitplate.fitplateapi.mealplan.domain.MealPlanNamer;
import com.fitplate.fitplateapi.mealplan.dto.FoodAlternativesResponse;
import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.dto.MealResponse;
import com.fitplate.fitplateapi.mealplan.dto.SavedMealPlanResponse;
import com.fitplate.fitplateapi.mealplan.repository.MealPlanRepository;
import com.fitplate.fitplateapi.personalization.service.PersonalizationService;
import com.fitplate.fitplateapi.user.domain.FoodCategory;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import com.fitplate.fitplateapi.user.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class MealPlanEditService {

    static final int ALTERNATIVE_COUNT = 3;
    /** 대안으로 보여줄 수 있는 칼로리 차이. AI 에는 ±10% 를 요청한다. */
    static final double ALTERNATIVE_CALORIE_TOLERANCE = 0.15;
    /** 교체 저장을 허용하는 칼로리 차이. */
    static final double REPLACEMENT_CALORIE_TOLERANCE = 0.20;
    /** AI 에 한 번에 받는 대안 수. 남는 것은 저장해 「다른 후보 보기」에서 먼저 쓴다. */
    static final int AI_ALTERNATIVE_COUNT = 6;
    /** AI 에 넘기는 '제안하지 말 음식' 최대 개수. 프롬프트가 지나치게 길어지지 않게 한다. */
    static final int MAX_AVOID_NAMES = 40;

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanOwnership ownership;
    private final UserProfileService userProfileService;
    private final MealPlanAiClient mealPlanAiClient;
    private final MealPlanValidator mealPlanValidator;
    private final PersonalizationService personalizationService;
    private final FoodAlternativePool alternativePool;
    private final AiUsageQuota aiUsageQuota;
    private final TransactionTemplate readOnlyTransaction;
    private final TransactionTemplate writeTransaction;

    public MealPlanEditService(
            MealPlanRepository mealPlanRepository,
            MealPlanOwnership ownership,
            UserProfileService userProfileService,
            MealPlanAiClient mealPlanAiClient,
            MealPlanValidator mealPlanValidator,
            PersonalizationService personalizationService,
            FoodAlternativePool alternativePool,
            AiUsageQuota aiUsageQuota,
            PlatformTransactionManager transactionManager
    ) {
        this.mealPlanRepository = mealPlanRepository;
        this.ownership = ownership;
        this.userProfileService = userProfileService;
        this.mealPlanAiClient = mealPlanAiClient;
        this.mealPlanValidator = mealPlanValidator;
        this.personalizationService = personalizationService;
        this.alternativePool = alternativePool;
        this.aiUsageQuota = aiUsageQuota;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
        this.writeTransaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 한 끼를 기존 끼니 합계에 맞춰 AI 로 새로 구성한다. AI 호출은 트랜잭션 밖에서 하고,
     * 검증에 실패하면 한 번 더 요청한 뒤 형식이 맞는 것 중 목표에 더 가까운 쪽으로 바꾼다.
     */
    public SavedMealPlanResponse regenerateMeal(String tossUserKey, Long mealPlanId, String mealType) {
        MealSlot slot = readOnlyTransaction.execute(status -> loadMealSlot(tossUserKey, mealPlanId, mealType));
        FoodPreferences preferences = userProfileService.getFoodPreferences(tossUserKey);
        PersonalizationHints hints = personalizationService.hintsFor(tossUserKey, preferences);
        aiUsageQuota.acquire(tossUserKey, AiFeature.MEAL_REGENERATION);
        MealResponse regenerated;
        try {
            regenerated = generateValidatedMeal(slot, preferences, hints);
        } catch (RuntimeException e) {
            // 새 끼니를 주지 못했으면 횟수를 돌려준다
            aiUsageQuota.release(tossUserKey, AiFeature.MEAL_REGENERATION);
            throw e;
        }
        return writeTransaction.execute(status -> applyRegeneratedMeal(tossUserKey, mealPlanId, mealType, regenerated));
    }

    private MealResponse generateValidatedMeal(MealSlot slot, FoodPreferences preferences, PersonalizationHints hints) {
        MealResponse first = mealPlanAiClient.regenerateMeal(slot.context(preferences, hints, null));
        MealPlanValidator.Result firstResult = mealPlanValidator.validateMeal(first, slot.mealType(), slot.target(), preferences);
        if (firstResult.isAcceptable()) {
            return first;
        }
        log.warn("끼니 재생성 검증 실패, 재요청. problems={}", firstResult.problems());

        MealResponse second;
        try {
            second = mealPlanAiClient.regenerateMeal(slot.context(preferences, hints, firstResult.feedback()));
        } catch (RuntimeException e) {
            if (firstResult.isUsable()) {
                log.warn("끼니 재생성 재요청 실패, 첫 결과 사용. cause={}", e.getMessage());
                return first;
            }
            throw unavailable(firstResult, null);
        }
        MealPlanValidator.Result secondResult = mealPlanValidator.validateMeal(second, slot.mealType(), slot.target(), preferences);
        if (secondResult.isAcceptable()) {
            return second;
        }
        if (!firstResult.isUsable() && !secondResult.isUsable()) {
            throw unavailable(firstResult, secondResult);
        }
        log.warn("끼니 재생성 재요청 후에도 검증 실패. problems={}", secondResult.problems());
        if (!firstResult.isUsable()) {
            return second;
        }
        if (!secondResult.isUsable()) {
            return first;
        }
        return secondResult.score() <= firstResult.score() ? second : first;
    }

    private static RuntimeException unavailable(MealPlanValidator.Result first, MealPlanValidator.Result second) {
        boolean allergy = first.allergyViolation() || (second != null && second.allergyViolation());
        return allergy ? new AllergySafeResultUnavailableException() : new MealRegenerationUnavailableException();
    }

    private SavedMealPlanResponse applyRegeneratedMeal(String tossUserKey, Long mealPlanId, String mealType,
                                                       MealResponse regenerated) {
        MealPlan plan = findOwnedPlan(tossUserKey, mealPlanId);
        MealPlanMeal meal = findMeal(plan, mealType);
        ownership.requireNotEaten(meal);
        meal.clearFoods();
        mealPlanRepository.flush();

        meal.rename(regenerated.getTitle().isBlank() ? meal.getTitle() : regenerated.getTitle().trim());
        List<FoodItem> foods = regenerated.getFoods();
        for (int index = 0; index < foods.size(); index++) {
            FoodItem food = foods.get(index);
            meal.addFood(MealPlanFood.builder()
                    .name(food.getName().trim())
                    .amount(food.getAmount().trim())
                    .calories(food.getCalories())
                    .protein(toDecimal(food.getProtein()))
                    .carbohydrate(toDecimal(food.getCarbohydrate()))
                    .fat(toDecimal(food.getFat()))
                    .shoppingKeyword(keywordOrName(food))
                    .sortOrder(index)
                    .build());
        }
        return SavedMealPlanResponse.from(plan);
    }

    private MealSlot loadMealSlot(String tossUserKey, Long mealPlanId, String mealType) {
        MealPlan plan = findOwnedPlan(tossUserKey, mealPlanId);
        MealPlanMeal meal = findMeal(plan, mealType);
        ownership.requireNotEaten(meal);
        List<FoodItem> current = meal.getFoods().stream().map(FoodItem::from).toList();
        List<FoodItem> others = plan.getMeals().stream()
                .filter(other -> other != meal)
                .flatMap(other -> other.getFoods().stream())
                .map(FoodItem::from)
                .toList();
        MealTarget target = new MealTarget(
                current.stream().mapToInt(FoodItem::getCalories).sum(),
                current.stream().mapToDouble(FoodItem::getProtein).sum(),
                current.stream().mapToDouble(FoodItem::getCarbohydrate).sum(),
                current.stream().mapToDouble(FoodItem::getFat).sum()
        );
        return new MealSlot(plan.getGoal(), mealType, target, current, others);
    }

    public FoodAlternativesResponse suggestAlternatives(String tossUserKey, Long mealPlanId,
                                                        String mealType, int foodIndex) {
        return suggestAlternatives(tossUserKey, mealPlanId, mealType, foodIndex, List.of());
    }

    /**
     * 즐겨찾기 → 저장된 대안 → AI 순으로 3개를 채운다. shownNames 는 이번 시트에서 이미 보여준 음식으로,
     * 다시 내지 않는다. AI 는 앞의 두 곳으로 모자랄 때만 하루 상한 안에서 부르고, 받은 대안은 저장해 둔다.
     */
    public FoodAlternativesResponse suggestAlternatives(String tossUserKey, Long mealPlanId, String mealType,
                                                        int foodIndex, List<String> shownNames) {
        FoodSlot slot = readOnlyTransaction.execute(status -> loadSlot(tossUserKey, mealPlanId, mealType, foodIndex));
        FoodPreferences preferences = userProfileService.getFoodPreferences(tossUserKey);
        Set<String> shown = new HashSet<>();
        (shownNames == null ? List.<String>of() : shownNames).forEach(name -> shown.add(FoodAlternativePool.key(name)));

        List<FoodItem> accepted = new ArrayList<>(filterAlternatives(
                personalizationService.favoriteCandidates(tossUserKey), slot, preferences, List.of(), shown));
        List<String> favoriteNames = accepted.stream().map(FoodItem::getName).toList();
        List<FoodItem> pooled = alternativePool.find(slot.target(), ALTERNATIVE_CALORIE_TOLERANCE);
        accepted.addAll(filterAlternatives(pooled, slot, preferences, accepted, shown));
        if (accepted.size() == ALTERNATIVE_COUNT) {
            return new FoodAlternativesResponse(slot.target(), accepted, favoriteNames);
        }

        try {
            aiUsageQuota.acquire(tossUserKey, AiFeature.ALTERNATIVES);
        } catch (AiDailyLimitExceededException e) {
            if (!accepted.isEmpty()) {
                return new FoodAlternativesResponse(slot.target(), accepted, favoriteNames);
            }
            throw e;
        }

        // 이미 보여줬거나 저장돼 있거나 사용자가 최근에 뺀 음식은 AI 에 다시 제안하지 말라고 넘긴다
        Set<String> avoid = new LinkedHashSet<>();
        if (shownNames != null) {
            avoid.addAll(shownNames);
        }
        accepted.forEach(food -> avoid.add(food.getName()));
        pooled.forEach(food -> avoid.add(food.getName()));
        PersonalizationHints hints = personalizationService.hintsFor(tossUserKey, preferences);
        if (hints != null) {
            avoid.addAll(hints.swappedOutFoods());
        }
        List<String> avoidNames = new ArrayList<>(avoid.stream().limit(MAX_AVOID_NAMES).toList());
        try {
            List<FoodItem> fromAi = requestAlternatives(slot, preferences, avoidNames, accepted, shown);
            if (fromAi.isEmpty()) {
                log.warn("음식 대안이 조건을 통과하지 못해 재요청. mealPlanId={}, food={}", mealPlanId, slot.target().getName());
                fromAi = requestAlternatives(slot, preferences, avoidNames, accepted, shown);
            }
            accepted.addAll(fromAi);
        } catch (RuntimeException e) {
            aiUsageQuota.release(tossUserKey, AiFeature.ALTERNATIVES);
            if (accepted.isEmpty()) {
                throw e;
            }
            log.warn("AI 대안 요청 실패, 저장된 대안만 사용. cause={}", e.getMessage());
        }
        if (accepted.isEmpty()) {
            throw shown.isEmpty()
                    ? new FoodAlternativesUnavailableException()
                    : new FoodAlternativesUnavailableException("새로운 음식을 더 찾지 못했어요. 지금 보이는 후보 중에서 골라 주세요.");
        }
        return new FoodAlternativesResponse(slot.target(), accepted, favoriteNames);
    }

    /** 식단 이름을 바꾼다. 비우면 자동 이름으로 돌아간다. */
    @Transactional
    public SavedMealPlanResponse renamePlan(String tossUserKey, Long mealPlanId, String title) {
        MealPlan plan = findOwnedPlan(tossUserKey, mealPlanId);
        if (title != null && title.trim().length() > MealPlanNamer.MAX_LENGTH) {
            throw new InvalidRequestException("식단 이름은 %d자까지 쓸 수 있어요.".formatted(MealPlanNamer.MAX_LENGTH));
        }
        plan.rename(title);
        return SavedMealPlanResponse.from(plan);
    }

    @Transactional
    public SavedMealPlanResponse replaceFood(String tossUserKey, Long mealPlanId, String mealType,
                                             int foodIndex, FoodItem replacement) {
        MealPlan plan = findOwnedPlan(tossUserKey, mealPlanId);
        MealPlanFood food = findFood(plan, mealType, foodIndex);
        ownership.requireNotEaten(food.getMeal());

        if (!MealPlanValidator.isWellFormed(replacement)) {
            throw new InvalidFoodReplacementException("바꿀 음식의 이름·분량·영양 정보가 올바르지 않습니다.");
        }
        if (!withinTolerance(replacement.getCalories(), food.getCalories(), REPLACEMENT_CALORIE_TOLERANCE)) {
            throw new InvalidFoodReplacementException("기존 음식과 칼로리 차이가 커서 식단 균형이 깨집니다.");
        }
        FoodCategory allergy = userProfileService.getFoodPreferences(tossUserKey)
                .findAllergyMatch(replacement.getName(), replacement.getShoppingKeyword());
        if (allergy != null) {
            throw new InvalidFoodReplacementException("알레르기로 설정한 분류(%s)의 음식이라 넣을 수 없습니다.".formatted(allergy.getLabel()));
        }

        personalizationService.recordReplacement(plan.getUser(), food.getName(), replacement.getName().trim());
        food.replaceWith(
                replacement.getName().trim(),
                replacement.getAmount().trim(),
                replacement.getCalories(),
                toDecimal(replacement.getProtein()),
                toDecimal(replacement.getCarbohydrate()),
                toDecimal(replacement.getFat()),
                keywordOrName(replacement)
        );
        return SavedMealPlanResponse.from(plan);
    }

    private List<FoodItem> requestAlternatives(FoodSlot slot, FoodPreferences preferences, List<String> avoidNames,
                                               List<FoodItem> already, Set<String> shown) {
        List<FoodItem> candidates = mealPlanAiClient.suggestAlternatives(new FoodAlternativeContext(
                slot.goal(), slot.mealType(), slot.otherFoods(), slot.target(),
                preferences, AI_ALTERNATIVE_COUNT, List.copyOf(avoidNames)
        ));
        alternativePool.save(slot.target(), candidates, ALTERNATIVE_CALORIE_TOLERANCE);
        List<FoodItem> accepted = filterAlternatives(candidates, slot, preferences, already, shown);
        // 재요청 때 걸러진 후보를 다시 내지 않도록 이름을 넘긴다
        candidates.stream()
                .filter(MealPlanValidator::isWellFormed)
                .map(FoodItem::getName)
                .forEach(avoidNames::add);
        return accepted;
    }

    /**
     * already 는 이미 고른 대안, shown 은 이전에 보여준 음식 키다. 둘과 겹치지 않게,
     * 합쳐서 ALTERNATIVE_COUNT 개까지만 고른다.
     */
    List<FoodItem> filterAlternatives(List<FoodItem> candidates, FoodSlot slot, FoodPreferences preferences,
                                      List<FoodItem> already, Set<String> shown) {
        Set<String> taken = new HashSet<>(shown);
        taken.add(normalize(slot.target().getName()));
        slot.otherFoods().forEach(food -> taken.add(normalize(food.getName())));
        already.forEach(food -> taken.add(normalize(food.getName())));

        List<FoodItem> accepted = new ArrayList<>();
        for (FoodItem candidate : candidates == null ? List.<FoodItem>of() : candidates) {
            if (accepted.size() + already.size() >= ALTERNATIVE_COUNT) {
                break;
            }
            if (!MealPlanValidator.isWellFormed(candidate)
                    || !withinTolerance(candidate.getCalories(), slot.target().getCalories(), ALTERNATIVE_CALORIE_TOLERANCE)
                    || preferences.findExcludedMatch(candidate.getName(), candidate.getShoppingKeyword()) != null
                    || !taken.add(normalize(candidate.getName()))) {
                continue;
            }
            accepted.add(FoodItem.builder()
                    .name(candidate.getName().trim())
                    .amount(candidate.getAmount().trim())
                    .calories(candidate.getCalories())
                    .protein(candidate.getProtein())
                    .carbohydrate(candidate.getCarbohydrate())
                    .fat(candidate.getFat())
                    .shoppingKeyword(keywordOrName(candidate))
                    .build());
        }
        return accepted;
    }

    private FoodSlot loadSlot(String tossUserKey, Long mealPlanId, String mealType, int foodIndex) {
        MealPlan plan = findOwnedPlan(tossUserKey, mealPlanId);
        MealPlanFood food = findFood(plan, mealType, foodIndex);
        ownership.requireNotEaten(food.getMeal());
        List<FoodItem> otherFoods = food.getMeal().getFoods().stream()
                .filter(other -> other != food)
                .map(FoodItem::from)
                .toList();
        return new FoodSlot(plan.getGoal(), mealType, otherFoods, FoodItem.from(food));
    }

    private MealPlan findOwnedPlan(String tossUserKey, Long mealPlanId) {
        return ownership.findOwnedPlan(tossUserKey, mealPlanId);
    }

    private MealPlanMeal findMeal(MealPlan plan, String mealType) {
        return ownership.findMeal(plan, mealType);
    }

    private MealPlanFood findFood(MealPlan plan, String mealType, int foodIndex) {
        MealPlanMeal meal = findMeal(plan, mealType);
        if (foodIndex < 0 || foodIndex >= meal.getFoods().size()) {
            throw new ResourceNotFoundException(foodIndex, "해당 음식을 찾을 수 없습니다");
        }
        return meal.getFoods().get(foodIndex);
    }

    private static boolean withinTolerance(Integer calories, Integer original, double tolerance) {
        if (calories == null || original == null) {
            return false;
        }
        double allowed = Math.max(original * tolerance, 20);
        return Math.abs(calories - original) <= allowed;
    }

    private static String normalize(String name) {
        return FoodAlternativePool.key(name);
    }

    private static String keywordOrName(FoodItem food) {
        return food.getShoppingKeyword() == null || food.getShoppingKeyword().isBlank()
                ? food.getName().trim()
                : food.getShoppingKeyword().trim();
    }

    private static BigDecimal toDecimal(Double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    record FoodSlot(String goal, String mealType, List<FoodItem> otherFoods, FoodItem target) {
    }

    record MealSlot(String goal, String mealType, MealTarget target, List<FoodItem> currentFoods,
                    List<FoodItem> otherMealFoods) {
        MealRegenerationContext context(FoodPreferences preferences, PersonalizationHints hints, String feedback) {
            return new MealRegenerationContext(goal, mealType, target, currentFoods, otherMealFoods, preferences, hints, feedback);
        }
    }
}
