// 날짜별 식단 섭취 기록과 영양 합계를 서버에서 계산합니다. 기록이 없는 날을 섭취 0 으로 만들지 않습니다.
package com.fitplate.fitplateapi.record.service;

import com.fitplate.fitplateapi.exception.InvalidRequestException;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import com.fitplate.fitplateapi.mealplan.domain.MealPlanFood;
import com.fitplate.fitplateapi.mealplan.domain.MealPlanMeal;
import com.fitplate.fitplateapi.mealplan.repository.MealPlanRepository;
import com.fitplate.fitplateapi.mealplan.service.MealPlanOwnership;
import com.fitplate.fitplateapi.record.dto.DailyRecordResponse;
import com.fitplate.fitplateapi.record.dto.DailyRecordResponse.MealRecord;
import com.fitplate.fitplateapi.record.dto.DailyRecordResponse.RecordStatus;
import com.fitplate.fitplateapi.record.dto.NutritionTotals;
import com.fitplate.fitplateapi.record.dto.RecordsResponse;
import com.fitplate.fitplateapi.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyRecordService {

    static final int MAX_DAYS = 93;

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanOwnership ownership;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RecordsResponse getRecords(String tossUserKey, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new InvalidRequestException("조회 기간이 올바르지 않습니다.");
        }
        long totalDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (totalDays > MAX_DAYS) {
            throw new InvalidRequestException("한 번에 " + MAX_DAYS + "일까지 조회할 수 있습니다.");
        }

        User user = ownership.findUser(tossUserKey);
        Map<LocalDate, MealPlan> plansByDate = mealPlanRepository
                .findByUserAndPlanDateBetweenOrderByPlanDateAsc(user, from, to)
                .stream()
                .collect(Collectors.toMap(MealPlan::getPlanDate, Function.identity()));

        LocalDate today = LocalDate.now(clock);
        List<DailyRecordResponse> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            days.add(toDailyRecord(date, plansByDate.get(date), today));
        }
        return new RecordsResponse(from, to, days, summarize(days, (int) totalDays));
    }

    private DailyRecordResponse toDailyRecord(LocalDate date, MealPlan plan, LocalDate today) {
        if (plan == null) {
            return new DailyRecordResponse(date, RecordStatus.NO_PLAN, null, null, 0, 0,
                    null, null, null, null, List.of());
        }

        NutritionTotals planned = NutritionTotals.ZERO;
        NutritionTotals eaten = NutritionTotals.ZERO;
        int eatenMeals = 0;
        List<MealRecord> meals = new ArrayList<>();
        for (MealPlanMeal meal : plan.getMeals()) {
            NutritionTotals mealTotals = totalsOf(meal);
            planned = planned.plus(mealTotals);
            if (meal.isEaten()) {
                eaten = eaten.plus(mealTotals);
                eatenMeals++;
            }
            meals.add(new MealRecord(meal.getMealType(), meal.getTitle(), meal.isEaten(), mealTotals.calories(),
                    meal.getFoods().stream().map(MealPlanFood::getName).toList()));
        }

        int plannedMeals = plan.getMeals().size();
        boolean upcoming = date.isAfter(today);
        RecordStatus status = upcoming ? RecordStatus.UPCOMING
                : eatenMeals == 0 ? RecordStatus.NOT_RECORDED
                : eatenMeals == plannedMeals ? RecordStatus.COMPLETE
                : RecordStatus.PARTIAL;
        Double completionRate = upcoming || plannedMeals == 0 ? null : (double) eatenMeals / plannedMeals;
        NutritionTotals target = new NutritionTotals(plan.getTargetCalories(), plan.getProteinGram(),
                plan.getCarbsGram(), plan.getFatGram());

        return new DailyRecordResponse(date, status, plan.getId(), plan.getGoal(), plannedMeals, eatenMeals,
                completionRate, target, planned.rounded(), eatenMeals == 0 ? null : eaten.rounded(), meals);
    }

    private RecordsResponse.Summary summarize(List<DailyRecordResponse> days, int totalDays) {
        List<DailyRecordResponse> plannedDays = days.stream().filter(day -> day.completionRate() != null).toList();
        List<DailyRecordResponse> recordedDays = days.stream().filter(day -> day.eaten() != null).toList();

        Double averageCompletionRate = plannedDays.isEmpty() ? null
                : plannedDays.stream().mapToDouble(DailyRecordResponse::completionRate).average().orElse(0);
        NutritionTotals averageEaten = recordedDays.isEmpty() ? null
                : recordedDays.stream().map(DailyRecordResponse::eaten)
                .reduce(NutritionTotals.ZERO, NutritionTotals::plus)
                .dividedBy(recordedDays.size());
        NutritionTotals averagePlanned = recordedDays.isEmpty() ? null
                : recordedDays.stream().map(DailyRecordResponse::planned)
                .reduce(NutritionTotals.ZERO, NutritionTotals::plus)
                .dividedBy(recordedDays.size());
        NutritionTotals averageTarget = recordedDays.isEmpty() ? null
                : recordedDays.stream().map(DailyRecordResponse::target)
                .reduce(NutritionTotals.ZERO, NutritionTotals::plus)
                .dividedBy(recordedDays.size());

        return new RecordsResponse.Summary(totalDays, plannedDays.size(), recordedDays.size(),
                averageCompletionRate, averageEaten, averagePlanned, averageTarget);
    }

    private static NutritionTotals totalsOf(MealPlanMeal meal) {
        NutritionTotals totals = NutritionTotals.ZERO;
        for (MealPlanFood food : meal.getFoods()) {
            totals = totals.plus(food.getCalories(), food.getProtein().doubleValue(),
                    food.getCarbohydrate().doubleValue(), food.getFat().doubleValue());
        }
        return totals;
    }
}
