package com.fitplate.fitplateapi.mealplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveMealPlanRequest {
    @NotNull
    private String tossUserKey;
    @NotNull
    private Integer height;
    @NotNull
    private Integer weight;
    @NotNull
    private Integer age;
    @NotNull
    private String gender;
    private Double bodyFatRate;
    @NotBlank
    private String goal;
    @NotNull
    private Integer periodDays;
    @NotBlank
    private String aiResponseJson;
}
