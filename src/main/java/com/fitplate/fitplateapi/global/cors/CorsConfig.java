package com.fitplate.fitplateapi.global.cors;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 설정 클래스. 프론트엔드의 API 호출 시 CORS 에러를 방지한다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * /api/** 경로에 대한 CORS 정책을 설정한다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(
                    "http://localhost:5173",
                    "http://127.0.0.1:5173",
                    "https://fitplate.apps.tossmini.com",
                    "https://fitplate.private-apps.tossmini.com"
            )
            .allowedMethods(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"   // CORS preflight
            )
            .allowedHeaders("*")
            .allowCredentials(false)
            .maxAge(3600);
    }
}

