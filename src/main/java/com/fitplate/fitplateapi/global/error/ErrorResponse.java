package com.fitplate.fitplateapi.global.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 요청 실패 시 반환하는 에러 응답 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** HTTP 상태 코드 (400/404/500 등). */
    private Integer status;

    /** 사용자 친화적인 에러 메시지. */
    private String message;

    /** 에러 발생 시각 (ISO 8601). */
    private LocalDateTime timestamp;

    /** 에러가 발생한 API 요청 경로. */
    private String path;
}

