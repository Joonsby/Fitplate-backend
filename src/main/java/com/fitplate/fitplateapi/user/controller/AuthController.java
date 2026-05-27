package com.fitplate.fitplateapi.user.controller;

import com.fitplate.fitplateapi.user.dto.TossLoginRequest;
import com.fitplate.fitplateapi.user.dto.TossTokenResponse;
import com.fitplate.fitplateapi.user.dto.TossUserInfoResponse;
import com.fitplate.fitplateapi.user.service.TossAuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final TossAuthClient tossAuthClient;

    @PostMapping("/toss-login")
    public ResponseEntity<TossUserInfoResponse> tossLogin(@RequestBody TossLoginRequest request) {
        log.info("[TossLogin] 요청 수신");

        TossTokenResponse tokenResponse = tossAuthClient.generateToken(request);

        if (tokenResponse == null ||
                tokenResponse.getSuccess() == null ||
                tokenResponse.getSuccess().getAccessToken() == null) {
            log.warn("[TossLogin] accessToken 발급 실패");

            if (tokenResponse != null && tokenResponse.getError() != null) {
                log.warn("[TossLogin] errorCode={}", tokenResponse.getError().getErrorCode());
                log.warn("[TossLogin] reason={}", tokenResponse.getError().getReason());
            }

            return ResponseEntity.badRequest().build();
        }

        String accessToken = tokenResponse.getSuccess().getAccessToken();
        TossUserInfoResponse userInfoResponse = tossAuthClient.getUserInfo(accessToken);

        if (userInfoResponse != null && userInfoResponse.getSuccess() != null) {
            log.info("[TossLogin] tossUserKey={}", userInfoResponse.getSuccess().getUserKey());
        }

        return ResponseEntity.ok(userInfoResponse);
    }
}