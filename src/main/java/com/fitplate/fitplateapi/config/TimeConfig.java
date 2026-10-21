// 날짜 판단(오늘, 섭취 시각)을 한국 시간 기준으로 맞추는 Clock 을 제공합니다.
package com.fitplate.fitplateapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(SEOUL);
    }
}
