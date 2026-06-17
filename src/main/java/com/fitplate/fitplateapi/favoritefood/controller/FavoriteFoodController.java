package com.fitplate.fitplateapi.favoritefood.controller;

import com.fitplate.fitplateapi.favoritefood.dto.FavoriteFoodResponse;
import com.fitplate.fitplateapi.favoritefood.dto.FavoriteFoodToggleRequest;
import com.fitplate.fitplateapi.favoritefood.dto.FavoriteFoodToggleResponse;
import com.fitplate.fitplateapi.favoritefood.service.FavoriteFoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorite-foods")
@RequiredArgsConstructor
public class FavoriteFoodController {

    private final FavoriteFoodService favoriteFoodService;

    @GetMapping
    public List<FavoriteFoodResponse> getFavoriteFoods(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return favoriteFoodService.getFavoriteFoods(authorizationHeader);
    }

    @PostMapping("/toggle")
    public FavoriteFoodToggleResponse toggleFavoriteFood(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody FavoriteFoodToggleRequest request
    ) {
        return favoriteFoodService.toggleFavoriteFood(authorizationHeader, request);
    }

    @DeleteMapping("/{favoriteFoodId}")
    public void deleteFavoriteFood(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long favoriteFoodId
    ) {
        favoriteFoodService.deleteFavoriteFood(authorizationHeader, favoriteFoodId);
    }
}