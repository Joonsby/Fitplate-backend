package com.fitplate.fitplateapi.user.service;

import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.nutrition.service.NutritionCalculator;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.dto.UserProfileResponse;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final NutritionCalculator nutritionCalculator;

    @Transactional(readOnly = true)
    public Optional<UserProfileResponse> getMyProfile(String tossUserKey) {
        User user = userRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new ResourceNotFoundException(tossUserKey, "사용자를 찾을 수 없습니다"));

        return userProfileRepository.findByUser(user)
                .map(UserProfileResponse::from);
    }

    @Transactional
    public void upsertFromMealPlanRequest(String tossUserKey,MealPlanRequest request){
        User user = userRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new ResourceNotFoundException(tossUserKey, "사용자를 찾을 수 없습니다"));

        BigDecimal bmi = nutritionCalculator.calculateBmi(request.getHeight(),request.getWeight());
        BigDecimal bodyFatRate = toBigDecimal(request.getBodyFatRate());

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElse(null);

        if(profile == null){
            UserProfile newProfile = UserProfile.builder()
                    .user(user)
                    .height(request.getHeight())
                    .weight(request.getWeight())
                    .age(request.getAge())
                    .gender(request.getGender())
                    .bmi(bmi)
                    .bodyFatRate(bodyFatRate)
                    .build();
            userProfileRepository.save(newProfile);
            return;
        }

        profile.update(
                request.getHeight(),
                request.getWeight(),
                request.getAge(),
                request.getGender(),
                bmi,
                bodyFatRate
        );
    }

    private BigDecimal toBigDecimal(Double value) {
        // Double을 소수점 2자리 BigDecimal로 변환 (null이면 null 반환).
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
