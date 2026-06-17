package com.fitplate.fitplateapi.favoritefood.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FavoriteFoodToggleResponse {

    private boolean favorited;
    private String action; // ADDED or REMOVED
    private Long favoriteFoodId;
}