// 다른 사용자의 식단에 접근할 때 던지는 예외입니다 (403).
package com.fitplate.fitplateapi.exception;

public class MealPlanAccessDeniedException extends RuntimeException {
    public MealPlanAccessDeniedException() {
        super("해당 식단에 대한 권한이 없습니다.");
    }
}
