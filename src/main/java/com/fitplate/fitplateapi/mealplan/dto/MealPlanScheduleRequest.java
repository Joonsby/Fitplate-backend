// 식단을 기록할 날짜를 정하는 요청입니다. planDate 가 null 이면 지정을 해제합니다.
package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanScheduleRequest {
    private LocalDate planDate;

    /** 같은 날짜에 다른 식단이 있으면 그 식단 대신 이 식단으로 바꾼다. */
    private boolean replace;
}
