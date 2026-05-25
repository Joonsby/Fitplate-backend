package com.fitplate.fitplateapi.user.service;

import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public void upsertFromMealPlanRequest(MealPlanRequest request){
        Optional<UserProfile> optionalProfile = userProfileRepository.findByTossUserKey(request.getTossUserKey());

        BigDecimal bmi = calculateBmi(request.getHeight(),request.getWeight());

        BigDecimal bodyFatRate = toBigDecimal(
                request.getBodyFatRate()
        );

        if (optionalProfile.isPresent()) {
            UserProfile profile = optionalProfile.get();

            profile.update(
                    request.getHeight(),
                    request.getWeight(),
                    request.getAge(),
                    request.getGender(),
                    bmi,
                    bodyFatRate
            );

            return;
        }

        UserProfile profile = UserProfile.builder()
                .tossUserKey(request.getTossUserKey())
                .heightCm(request.getHeight())
                .weightKg(request.getWeight())
                .age(request.getAge())
                .gender(request.getGender())
                .bmi(bmi)
                .bodyFatRate(bodyFatRate)
                .build();

        userProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public UserProfile findByTossUserKey(String tossUserKey) {
        return userProfileRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자 프로필을 찾을 수 없습니다: " + tossUserKey
                ));
    }

    private BigDecimal calculateBmi(Integer height, Integer weight) {
        double heightM = height / 100.0;
        double bmi = weight / (heightM * heightM);
        return BigDecimal.valueOf(bmi).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
