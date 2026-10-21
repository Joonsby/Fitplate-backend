package com.fitplate.fitplateapi.user.service;

import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.nutrition.service.NutritionCalculator;
import com.fitplate.fitplateapi.user.domain.FoodCategory;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.dto.UserProfileResponse;
import com.fitplate.fitplateapi.user.dto.UserProfileUpdateRequest;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final NutritionCalculator nutritionCalculator;

    @Transactional(readOnly = true)
    public Optional<UserProfileResponse> getMyProfile(String tossUserKey) {
        User user = findUser(tossUserKey);

        return userProfileRepository.findByUser(user)
                .map(UserProfileResponse::from);
    }

    /** 프로필이 없으면 선호·제외·알레르기 없음으로 본다. */
    @Transactional(readOnly = true)
    public FoodPreferences getFoodPreferences(String tossUserKey) {
        User user = findUser(tossUserKey);
        return userProfileRepository.findByUser(user)
                .map(UserProfile::getFoodPreferences)
                .orElse(FoodPreferences.none());
    }

    @Transactional
    public void upsertFromMealPlanRequest(String tossUserKey,MealPlanRequest request){
        upsert(
                tossUserKey,
                request.getHeight(),
                request.getWeight(),
                request.getAge(),
                request.getGender(),
                request.getBodyFatRate(),
                request.getPreferredFoodCategories(),
                request.getExcludedFoodCategories(),
                request.getAllergyFoodCategories()
        );
    }

    @Transactional
    public UserProfileResponse updateMyProfile(String tossUserKey, UserProfileUpdateRequest request) {
        UserProfile profile = upsert(
                tossUserKey,
                request.getHeight(),
                request.getWeight(),
                request.getAge(),
                request.getGender(),
                request.getBodyFatRate(),
                request.getPreferredFoodCategories(),
                request.getExcludedFoodCategories(),
                request.getAllergyFoodCategories()
        );
        return UserProfileResponse.from(profile);
    }

    private UserProfile upsert(
            String tossUserKey,
            Integer height,
            Integer weight,
            Integer age,
            String gender,
            Double bodyFatRateValue,
            List<FoodCategory> preferred,
            List<FoodCategory> excluded,
            List<FoodCategory> allergies
    ) {
        User user = findUser(tossUserKey);

        BigDecimal bmi = nutritionCalculator.calculateBmi(height, weight);
        BigDecimal bodyFatRate = toBigDecimal(bodyFatRateValue);

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElse(null);

        if(profile == null){
            profile = UserProfile.builder()
                    .user(user)
                    .height(height)
                    .weight(weight)
                    .age(age)
                    .gender(gender)
                    .bmi(bmi)
                    .bodyFatRate(bodyFatRate)
                    .build();
            profile.updateFoodPreferences(toSet(preferred), toSet(excluded), toSet(allergies));
            return userProfileRepository.save(profile);
        }

        profile.update(height, weight, age, gender, bmi, bodyFatRate);
        profile.updateFoodPreferences(toSet(preferred), toSet(excluded), toSet(allergies));
        return profile;
    }

    private User findUser(String tossUserKey) {
        return userRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new ResourceNotFoundException(tossUserKey, "사용자를 찾을 수 없습니다"));
    }

    private Set<FoodCategory> toSet(List<FoodCategory> categories) {
        if (categories == null) {
            return null;
        }
        EnumSet<FoodCategory> result = EnumSet.noneOf(FoodCategory.class);
        categories.stream().filter(java.util.Objects::nonNull).forEach(result::add);
        return result;
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
