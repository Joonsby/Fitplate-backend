package com.fitplate.fitplateapi.favoritefood.service;

import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.favoritefood.domain.FavoriteFood;
import com.fitplate.fitplateapi.favoritefood.dto.FavoriteFoodResponse;
import com.fitplate.fitplateapi.favoritefood.dto.FavoriteFoodToggleRequest;
import com.fitplate.fitplateapi.favoritefood.dto.FavoriteFoodToggleResponse;
import com.fitplate.fitplateapi.favoritefood.repository.FavoriteFoodRepository;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteFoodService {

    private final FavoriteFoodRepository favoriteFoodRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public List<FavoriteFoodResponse> getFavoriteFoods(String authorizationHeader) {
        User user = getUserByToken(authorizationHeader);

        return favoriteFoodRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(FavoriteFoodResponse::from)
                .toList();
    }

    @Transactional
    public FavoriteFoodToggleResponse toggleFavoriteFood(
            String authorizationHeader,
            FavoriteFoodToggleRequest request
    ) {
        User user = getUserByToken(authorizationHeader);

        return favoriteFoodRepository
                .findByUserAndFoodNameAndAmount(
                        user,
                        request.getName(),
                        request.getAmount()
                )
                .map(existingFood -> {
                    Long deletedId = existingFood.getFavoriteFoodId();
                    favoriteFoodRepository.delete(existingFood);

                    return FavoriteFoodToggleResponse.builder()
                            .favorited(false)
                            .action("REMOVED")
                            .favoriteFoodId(deletedId)
                            .build();
                })
                .orElseGet(() -> {
                    FavoriteFood favoriteFood = FavoriteFood.builder()
                            .user(user)
                            .foodName(request.getName())
                            .amount(request.getAmount())
                            .calories(request.getCalories())
                            .carbohydrate(request.getCarbohydrate())
                            .protein(request.getProtein())
                            .fat(request.getFat())
                            .shoppingCategory(request.getShoppingCategory())
                            .shoppingKeyword(request.getShoppingKeyword())
                            .sourceFoodId(request.getSourceFoodId())
                            .build();

                    FavoriteFood saved = favoriteFoodRepository.save(favoriteFood);

                    return FavoriteFoodToggleResponse.builder()
                            .favorited(true)
                            .action("ADDED")
                            .favoriteFoodId(saved.getFavoriteFoodId())
                            .build();
                });
    }

    @Transactional
    public void deleteFavoriteFood(String authorizationHeader, Long favoriteFoodId) {
        User user = getUserByToken(authorizationHeader);

        FavoriteFood favoriteFood = favoriteFoodRepository
                .findByFavoriteFoodIdAndUser(favoriteFoodId, user)
                .orElseThrow(() -> new ResourceNotFoundException("즐겨찾기 음식을 찾을 수 없습니다."));

        favoriteFoodRepository.delete(favoriteFood);
    }

    private User getUserByToken(String authorizationHeader) {
        String token = jwtTokenProvider.resolveToken(authorizationHeader);
        String tossUserKey = jwtTokenProvider.getTossUserKey(token);

        return userRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }
}