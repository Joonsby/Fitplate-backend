package com.fitplate.fitplateapi.user.dto;

import com.fitplate.fitplateapi.user.domain.FoodCategory;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class UserProfileResponse {

    private Integer height;
    private Integer weight;
    private Integer age;
    private String gender;
    private BigDecimal bmi;
    private BigDecimal bodyFatRate;
    private List<FoodCategory> preferredFoodCategories;
    private List<FoodCategory> excludedFoodCategories;
    private List<FoodCategory> allergyFoodCategories;

    public static UserProfileResponse from(UserProfile profile) {
        FoodPreferences preferences = profile.getFoodPreferences();
        return UserProfileResponse.builder()
                .height(profile.getHeight())
                .weight(profile.getWeight())
                .age(profile.getAge())
                .gender(profile.getGender())
                .bmi(profile.getBmi())
                .bodyFatRate(profile.getBodyFatRate())
                .preferredFoodCategories(List.copyOf(preferences.preferred()))
                .excludedFoodCategories(List.copyOf(preferences.excluded()))
                .allergyFoodCategories(List.copyOf(preferences.allergies()))
                .build();
    }
}
