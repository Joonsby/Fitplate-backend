// 오늘 식단 생성 가능 횟수와 광고가 필요한지 알려주는 응답입니다. 하루 첫 생성은 광고 없이 만든다.
package com.fitplate.fitplateapi.mealplan.dto;

public record GenerationQuotaResponse(int usedToday, int dailyLimit, int remaining, boolean adRequired) {

    public static GenerationQuotaResponse of(int usedToday, int dailyLimit) {
        return new GenerationQuotaResponse(usedToday, dailyLimit, Math.max(dailyLimit - usedToday, 0), usedToday > 0);
    }
}
