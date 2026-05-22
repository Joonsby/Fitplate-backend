package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveMealPlanRequest {
    private String height;
    private String weight;
    private String gender;
    private String age;
    private String bodyFatRate;
    private String goal;
    private String periodDays;
}
