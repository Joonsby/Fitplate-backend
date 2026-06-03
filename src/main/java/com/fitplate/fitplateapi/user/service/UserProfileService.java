package com.fitplate.fitplateapi.user.service;

import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.nutrition.service.NutritionCalculator;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final NutritionCalculator nutritionCalculator;

    @Transactional
    public void upsertFromMealPlanRequest(String tossUserKey,MealPlanRequest request){
        log.info("[UserProfileService] tossUserKey={}", tossUserKey);
        if (tossUserKey == null || tossUserKey.isBlank()) {
            throw new IllegalArgumentException("tossUserKey가 비어 있습니다.");
        }
        Optional<UserProfile> optionalProfile = userProfileRepository.findByTossUserKey(tossUserKey);

        BigDecimal bmi = nutritionCalculator.calculateBmi(request.getHeight(),request.getWeight());
        BigDecimal bodyFatRate = toBigDecimal(request.getBodyFatRate());


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
                .tossUserKey(tossUserKey)
                .heightCm(request.getHeight())
                .weightKg(request.getWeight())
                .age(request.getAge())
                .gender(request.getGender())
                .bmi(bmi)
                .bodyFatRate(bodyFatRate)
                .build();

        userProfileRepository.save(profile);
    }

    private BigDecimal toBigDecimal(Double value) {
        // Double 값을 소수점 2자리 BigDecimal로 변환합니다.
        // 값이 null이면 null을 반환합니다.
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
