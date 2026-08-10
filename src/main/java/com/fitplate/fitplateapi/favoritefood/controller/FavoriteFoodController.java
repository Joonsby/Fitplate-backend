package com.fitplate.fitplateapi.favoritefood.controller;

import com.fitplate.fitplateapi.favoritefood.dto.FavoriteFoodResponse;
import com.fitplate.fitplateapi.favoritefood.dto.FavoriteFoodToggleRequest;
import com.fitplate.fitplateapi.favoritefood.dto.FavoriteFoodToggleResponse;
import com.fitplate.fitplateapi.favoritefood.service.FavoriteFoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorite-foods")
@RequiredArgsConstructor
@Tag(
        name = "즐겨찾기 음식",
        description = "로그인 사용자의 즐겨찾기 음식 조회, 추가 및 삭제 기능"
)
public class FavoriteFoodController {

    private final FavoriteFoodService favoriteFoodService;

    @Operation(
            summary = "즐겨찾기 음식 목록 조회",
            description = """
                    로그인 사용자가 즐겨찾기에 등록한 음식 목록을 최신 등록순으로 조회합니다.

                    음식명, 섭취량, 영양 정보, 쇼핑 검색 정보와 등록 시각을 반환합니다.
                    등록된 음식이 없으면 빈 배열을 반환하며, 유효한 JWT가 필요합니다.
                    """
    )
    @GetMapping
    public List<FavoriteFoodResponse> getFavoriteFoods(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return favoriteFoodService.getFavoriteFoods(authorizationHeader);
    }

    @Operation(
            summary = "즐겨찾기 음식 토글",
            description = """
                    음식명(name)과 섭취량(amount)을 기준으로 즐겨찾기 상태를 전환합니다.

                    - 동일한 음식이 없으면 새로 등록하고 action=ADDED, favorited=true를 반환합니다.
                    - 동일한 음식이 있으면 기존 항목을 삭제하고 action=REMOVED, favorited=false를 반환합니다.

                    응답의 favoriteFoodId는 추가되거나 삭제된 즐겨찾기 항목의 ID이며, 유효한 JWT가 필요합니다.
                    """
    )
    @PostMapping("/toggle")
    public FavoriteFoodToggleResponse toggleFavoriteFood(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody FavoriteFoodToggleRequest request
    ) {
        return favoriteFoodService.toggleFavoriteFood(authorizationHeader, request);
    }

    @Operation(
            summary = "즐겨찾기 음식 삭제",
            description = """
                    favoriteFoodId에 해당하는 내 즐겨찾기 음식을 삭제합니다.

                    다른 사용자의 즐겨찾기는 삭제할 수 없습니다.
                    항목이 없거나 본인 소유가 아니면 404 Not Found를 반환하며, 유효한 JWT가 필요합니다.
                    성공 시 응답 본문 없이 반환합니다.
                    """
    )
    @DeleteMapping("/{favoriteFoodId}")
    public void deleteFavoriteFood(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long favoriteFoodId
    ) {
        favoriteFoodService.deleteFavoriteFood(authorizationHeader, favoriteFoodId);
    }
}
