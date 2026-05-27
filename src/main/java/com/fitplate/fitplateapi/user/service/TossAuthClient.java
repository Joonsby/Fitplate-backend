package com.fitplate.fitplateapi.user.service;

import com.fitplate.fitplateapi.user.dto.TossLoginRequest;
import com.fitplate.fitplateapi.user.dto.TossTokenResponse;
import com.fitplate.fitplateapi.user.dto.TossUserInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class TossAuthClient {

    private final RestClient restClient;

    public TossAuthClient(@Qualifier("tossRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public TossTokenResponse generateToken(TossLoginRequest request) {
        try {
            TossTokenResponse response = restClient.post()
                    .uri("/api-partner/v1/apps-in-toss/user/oauth2/generate-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TossTokenResponse.class);

            if (response != null && response.getError() != null) {
                log.warn("[TossAuthClient] generate-token 실패 errorCode={}", response.getError().getErrorCode());
                log.warn("[TossAuthClient] generate-token 실패 reason={}", response.getError().getReason());
            }

            return response;

        } catch (Exception e) {
            log.error("[TossAuthClient] generate-token 호출 실패", e);
            throw e;
        }
    }

    public TossUserInfoResponse getUserInfo(String accessToken) {
        try {
            TossUserInfoResponse response = restClient.get()
                    .uri("/api-partner/v1/apps-in-toss/user/oauth2/login-me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(TossUserInfoResponse.class);

            if (response != null && response.getError() != null) {
                log.warn("[TossAuthClient] login-me 실패 errorCode={}", response.getError().getErrorCode());
                log.warn("[TossAuthClient] login-me 실패 reason={}", response.getError().getReason());
            }

            return response;

        } catch (Exception e) {
            log.error("[TossAuthClient] login-me 호출 실패", e);
            throw e;
        }
    }
}