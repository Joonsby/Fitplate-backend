package com.fitplate.fitplateapi.user.controller;

import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.user.dto.UserProfileResponse;
import com.fitplate.fitplateapi.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/user-profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final JwtTokenProvider jwtTokenProvider;

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