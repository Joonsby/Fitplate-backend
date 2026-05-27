package com.fitplate.fitplateapi.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TossLoginRequest {

    private String authorizationCode;
    private String referrer;
}