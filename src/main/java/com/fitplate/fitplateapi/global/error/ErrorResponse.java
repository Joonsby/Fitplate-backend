package com.fitplate.fitplateapi.global.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 요청 실패 시 반환하는 에러 응답 DTO
 *
 * 📌 사용 흐름:
 * 요청 에러 발생 → GlobalExceptionHandler → ErrorResponse 생성 → JSON으로 응답
 *
 * 📌 JSON 응답 예시:
 * {
 *   "status": 400,
 *   "message": "키는 필수입니다, 체중은 20kg 이상이어야 합니다",
 *   "timestamp": "2024-05-20T10:30:15",
 *   "path": "/api/meal-plan"
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * HTTP 상태 코드
     *
     * 📌 주요 상태 코드:
     * - 400: 잘못된 요청 (Bad Request)
     *        클라이언트가 보낸 데이터 검증 실패
     * - 404: 찾을 수 없음 (Not Found)
     *        요청한 리소스가 없음
     * - 500: 서버 에러 (Internal Server Error)
     *        서버에서 예상치 못한 에러 발생
     *
     * Integer status: HTTP 상태코드
     */
    private Integer status;

    /**
     * 에러 메시지
     *
     * 📌 예시:
     * - "키는 필수입니다" (단일 에러)
     * - "키는 필수입니다, 체중은 20kg 이상이어야 합니다" (다중 에러)
     * - "서버 처리 중 오류가 발생했습니다" (서버 에러)
     *
     * 📌 클라이언트는 이 메시지를 사용자에게 표시
     *
     * String message: 사용자 친화적인 에러 메시지
     */
    private String message;

    /**
     * 에러 발생 시각
     *
     * 📌 포맷:
     * ISO 8601 형식 (예: 2024-05-20T10:30:15)
     *
     * 📌 용도:
     * - 클라이언트가 에러 발생 시간 추적
     * - 백엔드 로깅 및 모니터링
     * - 디버깅 시 시간 정보 활용
     *
     * LocalDateTime timestamp: 에러 발생 시각
     */
    private LocalDateTime timestamp;

    /**
     * 요청 경로
     *
     * 📌 예시:
     * "/api/meal-plan"
     * "/api/users/123"
     *
     * 📌 용도:
     * - 어느 API 엔드포인트에서 에러 발생했는지 파악
     * - 로깅 및 모니터링
     * - 클라이언트가 어느 요청이 실패했는지 알 수 있음
     *
     * String path: API 요청 경로
     */
    private String path;
}

