package com.fitplate.fitplateapi.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 요청 실패 시 반환하는 에러 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * HTTP 상태 코드
     */
    private Integer status;

    /**
     * 에러 메시지
     */
    private String message;

    /**
     * 에러 발생 시각
     */
    private LocalDateTime timestamp;

    /**
     * 요청 경로
     */
    private String path;
}

