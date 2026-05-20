package com.fitplate.fitplateapi.global.cors;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS(Cross-Origin Resource Sharing) 설정 클래스
 * 프론트엔드에서 백엔드의 API를 호출할 때 CORS 에러가 발생하지 않도록 설정합니다
 *
 * 📌 CORS 문제란?
 * - 브라우저의 보안 정책으로 인해 다른 도메인의 API 호출이 기본적으로 차단됨
 *
 * 📌 시나리오:
 * 프론트엔드 (localhost:5173) → 백엔드 (localhost:8080) 호출
 * → 프로토콜, 포트가 다르므로 CORS 정책 위반
 * → 브라우저에서 요청 차단
 * → CORS 설정으로 해결
 *
 * 📌 @Configuration:
 * - 이 클래스가 Spring 설정 클래스임을 표시
 * - 애플리케이션 시작 시 이 설정이 적용됨
 *
 * 📌 WebMvcConfigurer 구현:
 * - Spring MVC 설정을 커스터마이징하기 위한 인터페이스
 * - addCorsMappings() 메서드 구현하여 CORS 설정
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * CORS 정책을 설정합니다
     *
     * 📌 설정 내용:
     * 1. 어느 경로에 CORS 적용할 것인가: /api/** (모든 API)
     * 2. 어느 도메인에서 요청을 허용할 것인가: localhost:5173 등
     * 3. 어느 HTTP 메서드를 허용할 것인가: GET, POST, PUT, DELETE 등
     * 4. 어느 헤더를 허용할 것인가: 모든 헤더
     * 5. 쿠키/인증 정보를 포함할 것인가: false
     * 6. 캐시할 것인가: 3600초(1시간)
     *
     * @param registry CORS 설정을 등록하는 객체
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // /api/**로 시작하는 모든 경로에 CORS 설정 적용
        registry.addMapping("/api/**")
            // 요청을 허용할 도메인 (프론트엔드 주소)
            .allowedOrigins(
                "http://localhost:5173",      // React 개발 환경 (기본 포트)
                "http://127.0.0.1:5173"       // 같은 주소, 다른 형식
            )
            // 허용할 HTTP 메서드 (REST API에서 모든 기본 메서드 포함)
            .allowedMethods(
                "GET",      // 조회 (SELECT)
                "POST",     // 생성 (INSERT)
                "PUT",      // 전체 수정 (UPDATE)
                "PATCH",    // 부분 수정 (PARTIAL UPDATE)
                "DELETE",   // 삭제 (DELETE)
                "OPTIONS"   // CORS preflight 요청 (브라우저가 자동으로 보냄)
            )
            // 허용할 요청 헤더 ("*" = 모든 헤더 허용)
            .allowedHeaders("*")
            // 인증 정보(쿠키, 인증 토큰) 포함 여부
            .allowCredentials(false)  // false = 인증 정보 미포함 (필요시 true로 변경)
            // CORS 설정 캐시 시간 (초 단위)
            // 브라우저가 preflight 요청 결과를 3600초 동안 캐시하므로 매번 확인 안 함
            .maxAge(3600);
    }
}

