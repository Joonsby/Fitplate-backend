package com.fitplate.fitplateapi.global.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 애플리케이션 전역의 예외를 처리하는 핸들러
 *
 * 📌 장점:
 * - 모든 컨트롤러에서 발생한 예외를 한 곳에서 통일된 형식으로 처리
 * - 중복된 try-catch 코드 제거
 * - 예외에 따른 응답 형식 일관성 보장
 *
 * 📌 동작 흐름:
 * 요청 처리 중 예외 발생
 * ↓
 * Spring이 GlobalExceptionHandler의 적절한 @ExceptionHandler 메서드 찾음
 * ↓
 * 해당 메서드 실행
 * ↓
 * 에러 응답(JSON) 반환
 *
 * 📌 @RestControllerAdvice:
 * - @ControllerAdvice + @ResponseBody 합쳐진 것
 * - 모든 @RestController에서 발생한 예외를 처리
 * - 응답은 JSON 형식으로 직렬화
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Validation 실패로 인한 MethodArgumentNotValidException을 처리합니다
     *
     * 📌 발생 상황:
     * - API 요청의 @RequestBody 데이터에 @Valid 검증이 실패한 경우
     *
     * 📌 예시:
     * POST /api/meal-plan
     * {
     *   "height": 50,         // 검증 실패: 100 미만
     *   "weight": 200,        // OK
     *   "age": 30,            // OK
     *   "gender": "",         // 검증 실패: 빈 문자열
     *   "goal": "MUSCLE_GAIN",
     *   "periodDays": 7
     * }
     *
     * 📌 응답:
     * 400 Bad Request
     * {
     *   "status": 400,
     *   "message": "키는 100cm 이상이어야 합니다, 성별은 필수입니다",
     *   "timestamp": "2024-05-20T10:30:15",
     *   "path": "/api/meal-plan"
     * }
     *
     * @param ex 발생한 MethodArgumentNotValidException
     * @param request 현재 HTTP 요청 정보
     * @return 에러 응답 (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        // Step 1: Validation 에러 메시지 수집
        // BindingResult에서 모든 필드 에러를 가져옴
        // 각 에러의 기본 메시지를 쉼표로 구분하여 연결
        String errorMessage = ex.getBindingResult()
            .getFieldErrors()  // 검증 실패한 필드들
            .stream()          // 스트림으로 변환
            .map(error -> error.getDefaultMessage())  // 각 에러의 메시지 추출
            .collect(Collectors.joining(", "));  // 쉼표로 연결

        // Step 2: 에러 응답 객체 생성
        // ErrorResponse 빌더 패턴 사용
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())  // 400
            .message(errorMessage)  // "키는 100cm 이상이어야 합니다, 성별은 필수입니다"
            .timestamp(LocalDateTime.now())  // 현재 시간
            .path(request.getDescription(false).replace("uri=", ""))  // "/api/meal-plan"
            .build();

        // Step 3: ResponseEntity로 감싸서 응답
        // 400 상태코드 + ErrorResponse JSON 반환
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 기타 모든 예외를 처리합니다
     *
     * 📌 발생 상황:
     * - 예상치 못한 서버 에러
     * - NullPointerException, IllegalStateException 등
     * - 외부 API 호출 실패 (Gemini API 등)
     *
     * 📌 예시:
     * - JSON 변환 실패
     * - 데이터베이스 연결 실패
     * - AI API 호출 실패
     *
     * 📌 응답:
     * 500 Internal Server Error
     * {
     *   "status": 500,
     *   "message": "서버 처리 중 오류가 발생했습니다",
     *   "timestamp": "2024-05-20T10:30:15",
     *   "path": "/api/meal-plan"
     * }
     *
     * @param ex 발생한 예외
     * @param request 현재 HTTP 요청 정보
     * @return 에러 응답 (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        // Step 1: 에러 응답 객체 생성
        // 클라이언트에게 구체적인 서버 에러 정보는 숨기고 일반적인 메시지만 전달
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())  // 500
            .message("서버 처리 중 오류가 발생했습니다")  // 일반적인 메시지
            .timestamp(LocalDateTime.now())  // 현재 시간
            .path(request.getDescription(false).replace("uri=", ""))  // "/api/meal-plan"
            .build();

        // Step 2: ResponseEntity로 감싸서 응답
        // 500 상태코드 + ErrorResponse JSON 반환
        // 참고: ex.printStackTrace() 또는 로깅으로 상세 정보 기록 필요 (운영 환경에서)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

