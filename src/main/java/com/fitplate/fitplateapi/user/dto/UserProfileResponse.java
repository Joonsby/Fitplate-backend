package com.fitplate.fitplateapi.user.dto;

import com.fitplate.fitplateapi.user.domain.UserProfile;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class UserProfileResponse {

    private Integer height;
    private Integer weight;
    private Integer age;
    private String gender;
    private BigDecimal bmi;
    private BigDecimal bodyFatRate;

    public static UserProfileResponse from(UserProfile profile) {
        return UserProfileResponse.builder()
                .height(profile.getHeight())
                .weight(profile.getWeight())
                .age(profile.getAge())
                .gender(profile.getGender())
                .bmi(profile.getBmi())
                .bodyFatRate(profile.getBodyFatRate())
                .build();
    }
}