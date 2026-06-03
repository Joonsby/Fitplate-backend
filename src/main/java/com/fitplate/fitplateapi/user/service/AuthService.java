package com.fitplate.fitplateapi.user.service;

import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.user.dto.LoginResponse;
import com.fitplate.fitplateapi.user.dto.TossLoginRequest;
import com.fitplate.fitplateapi.user.dto.TossTokenResponse;
import com.fitplate.fitplateapi.user.dto.TossUserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TossAuthClient tossAuthClient;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse tossLogin(TossLoginRequest request) {
        TossTokenResponse tokenResponse = tossAuthClient.generateToken(request);

        if (tokenResponse == null ||
                tokenResponse.getSuccess() == null ||
                tokenResponse.getSuccess().getAccessToken() == null) {
            throw new IllegalArgumentException("토스 accessToken 발급 실패");
        }

        String accessToken = tokenResponse.getSuccess().getAccessToken();
        TossUserInfoResponse userInfoResponse = tossAuthClient.getUserInfo(accessToken);

        if (userInfoResponse == null || userInfoResponse.getSuccess() == null) {
            throw new IllegalArgumentException("토스 accessToken 발급 실패");
        }

        String tossUserKey = userInfoResponse.getSuccess().getUserKey();

        userService.findOrCreateByTossUserKey(tossUserKey);

        String jwt = jwtTokenProvider.createToken(tossUserKey);

        return new LoginResponse(jwt);
    }
}