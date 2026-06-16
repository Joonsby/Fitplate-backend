package com.fitplate.fitplateapi.exception;

/**
 * 모든 "Not Found" 상황을 통합 처리하는 예외.
 * 메시지 형식: "[message]: [resourceId]"
 */
public class ResourceNotFoundException extends RuntimeException {

    /** 리소스 ID를 메시지에 포함한 예외 생성. */
    public ResourceNotFoundException(Object resourceId, String message) {
        super(message + ": " + resourceId);
    }

    /** 메시지만 사용하는 예외 생성. */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

