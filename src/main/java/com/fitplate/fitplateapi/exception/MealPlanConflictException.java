// 식단 날짜·섭취 상태와 맞지 않는 요청에 던지는 예외입니다 (409). code 로 앱이 상황을 구분합니다.
package com.fitplate.fitplateapi.exception;

import lombok.Getter;

@Getter
public class MealPlanConflictException extends RuntimeException {

    /** 같은 날짜에 다른 식단이 이미 지정돼 있다. replace=true 로 바꿀 수 있다. */
    public static final String DATE_TAKEN = "DATE_TAKEN";
    /** 먹은 끼니가 있어 날짜를 바꾸거나 해제할 수 없다. */
    public static final String HAS_INTAKE = "HAS_INTAKE";
    /** 날짜를 지정하지 않은 식단은 섭취를 기록할 수 없다. */
    public static final String NOT_SCHEDULED = "NOT_SCHEDULED";
    /** 아직 오지 않은 날짜의 끼니는 먹었다고 기록할 수 없다. */
    public static final String FUTURE_DATE = "FUTURE_DATE";
    /** 먹은 끼니는 음식을 바꾸거나 다시 만들 수 없다. */
    public static final String MEAL_EATEN = "MEAL_EATEN";

    private final String code;

    public MealPlanConflictException(String code, String message) {
        super(message);
        this.code = code;
    }
}
