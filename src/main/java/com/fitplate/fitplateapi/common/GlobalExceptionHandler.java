package com.fitplate.fitplateapi.common;

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
 * 모든 컨트롤러에서 발생한 예외를 통일된 형식으로 반환합니다
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Validation 실패로 인한 MethodArgumentNotValidException을 처리합니다
     * 이는 @Valid 어노테이션을 사용한 요청 데이터 검증이 실패했을 때 발생합니다
     *
     * @param ex 발생한 예외
     * @param request 현재 요청 정보
     * @return 에러 응답 (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        // Validation 에러 메시지를 수집합니다
        // 예: "키는 필수입니다, 체중은 20kg 이상이어야 합니다"
        String errorMessage = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        // 에러 응답 객체 생성
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .message(errorMessage)
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        // 400 상태코드와 함께 에러 응답 반환
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 기타 모든 예외를 처리합니다
     *
     * @param ex 발생한 예외
     * @param request 현재 요청 정보
     * @return 에러 응답 (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        // 에러 응답 객체 생성
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("서버 처리 중 오류가 발생했습니다")
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        // 500 상태코드와 함께 에러 응답 반환
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

