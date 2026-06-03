package com.fitplate.fitplateapi.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TossUserInfoResponse {
    private String resultType;
    private Success success;
    private Error error;

    @Getter
    @NoArgsConstructor
    public static class Success {
        private String userKey;
        private String name;
        private String phone;
        private String birthday;
        private String ci;
        private String di;
    }

    @Getter
    @NoArgsConstructor
    public static class Error {
        private String errorCode;
        private String reason;
    }
}