package com.fitplate.fitplateapi.user.controller;

import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.user.dto.UserProfileResponse;
import com.fitplate.fitplateapi.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/user-profile")
@RequiredArgsConstructor
@Tag(
        name = "사용자 프로필",
        description = "로그인 사용자의 신체 정보 및 건강 지표 조회 기능"
)
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(
            summary = "내 프로필 조회",
            description = """
                    JWT에 포함된 사용자 키를 기준으로 내 프로필을 조회합니다.

                    프로필에는 키, 몸무게, 나이, 성별, BMI, 체지방률이 포함됩니다.
                    식단 생성 이력이 없어 프로필이 아직 등록되지 않은 경우 204 No Content를 반환합니다.
                    유효한 JWT가 필요합니다.
                    """
    )
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @RequestHeader("Authorization") String authorization
    ) {
        String tossUserKey = extractTossUserKey(authorization);

        Optional<UserProfileResponse> response =
                userProfileService.getMyProfile(tossUserKey);

        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private String extractTossUserKey(String authorization) {
        String token = jwtTokenProvider.resolveToken(authorization);
        return jwtTokenProvider.getTossUserKey(token);
    }
}
