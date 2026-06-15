package com.fitplate.fitplateapi.exception;

/**
 * 리소스가 존재하지 않을 때 발생하는 예외
 *
 * 📌 특징:
 * - 첫 번째 파라미터로 리소스 ID (Object)를 받음
 * - 두 번째 파라미터로 사용자 정의 메시지를 받음
 * - 하나의 Exception 클래스로 모든 "Not Found" 상황을 처리
 *
 * 📌 장점:
 * - 새로운 리소스가 추가될 때마다 Exception 클래스를 추가할 필요 없음
 * - 모든 Not Found 상황을 통일된 방식으로 처리
 * - 코드 유지보수 용이
 *
 * 📌 사용 예시:
 * throw new ResourceNotFoundException(id, "식단을 찾을 수 없습니다");
 * throw new ResourceNotFoundException(userId, "사용자를 찾을 수 없습니다");
 * throw new ResourceNotFoundException(userKey, "사용자 프로필을 찾을 수 없습니다");
 *
 * 📌 메시지 형식:
 * "[message]: [resourceId]"
 * 예: "식단을 찾을 수 없습니다: 123"
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * 리소스 ID를 포함한 예외 생성
     *
     * @param resourceId 찾을 수 없는 리소스의 식별자 (ID, Key 등)
     * @param message 사용자 정의 메시지
     */
    public ResourceNotFoundException(Object resourceId, String message) {
        super(message + ": " + resourceId);
    }

    /**
     * 메시지만 사용하는 예외 생성 (리소스 ID 없을 때)
     *
     * @param message 사용자 정의 메시지
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

