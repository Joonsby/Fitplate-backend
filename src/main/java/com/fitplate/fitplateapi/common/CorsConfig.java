package com.fitplate.fitplateapi.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS(Cross-Origin Resource Sharing) 설정 클래스
 * 프론트엔드에서 백엔드의 API를 호출할 때 CORS 에러가 발생하지 않도록 설정합니다
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * CORS 정책을 설정합니다
     *
     * @param registry CORS 설정을 등록하는 객체
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // /api 경로의 모든 요청에 CORS 허용
            .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")  // 로컬 개발 환경 허용 (React 기본 포트)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")  // 지원하는 HTTP 메서드
            .allowedHeaders("*")  // 모든 헤더 허용
            .allowCredentials(false)  // 쿠키 및 인증 정보 포함 허용
            .maxAge(3600);  // CORS 정책 캐시 시간 (초 단위)
    }
}

