package com.fitplate.fitplateapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Fitplate 애플리케이션의 메인 진입점
 *
 * 📌 Spring Boot 애플리케이션 실행 흐름:
 * 1. main() 메서드가 실행되면 SpringApplication.run()을 호출합니다
 * 2. 스프링 컨테이너가 초기화되고 모든 빈(Bean)이 등록됩니다
 * 3. 자동 설정(Auto Configuration)이 진행됩니다
 * 4. 내장 톰캣 서버가 시작되고 API 요청을 받을 준비가 됩니다
 *
 * @SpringBootApplication 어노테이션:
 * - @Configuration: 이 클래스가 설정 클래스임을 표시
 * - @EnableAutoConfiguration: 스프링이 클래스패스의 JAR 파일을 기반으로 자동 설정
 * - @ComponentScan: 현재 패키지 이하의 모든 @Component, @Service, @Repository 등을 자동으로 스캔
 */
@SpringBootApplication
public class FitplateApiApplication {

    /**
     * 애플리케이션 시작점
     *
     * @param args 명령어 인자 (전달되는 경우는 드물지만 필요시 처리 가능)
     */
    public static void main(String[] args) {
        // Spring Boot 애플리케이션 시작
        SpringApplication.run(FitplateApiApplication.class, args);
    }

}
