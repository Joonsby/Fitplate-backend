package com.fitplate.fitplateapi.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TossTokenResponse {
    private String resultType;
    private Success success;
    private Error error;

    @Getter
    @NoArgsConstructor
    public static class Success {
        private String accessToken;
    }

    @Getter
    @NoArgsConstructor
    public static class Error {
        private String errorCode;
        private String reason;
    }
}