// 기간 안의 날짜별 기록과, 기록이 있는 날만으로 계산한 기간 요약입니다.
package com.fitplate.fitplateapi.record.dto;

import java.time.LocalDate;
import java.util.List;

public record RecordsResponse(LocalDate from, LocalDate to, List<DailyRecordResponse> days, Summary summary) {

    /**
     * plannedDays 는 식단이 지정된 지난 날짜(오늘 포함) 수, recordedDays 는 한 끼 이상 체크한 날짜 수다.
     * 평균 섭취량·평균 식단 계획·평균 목표는 recordedDays 로만, 평균 완료율은 plannedDays 로 계산한다.
     */
    public record Summary(
            int totalDays,
            int plannedDays,
            int recordedDays,
            Double averageCompletionRate,
            NutritionTotals averageEaten,
            NutritionTotals averagePlanned,
            NutritionTotals averageTarget
    ) {
    }
}
