package com.fitplate.fitplateapi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DBHealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/api/health/db")
    public Map<String, Object> checkDbConnection() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        return Map.of(
                "status","OK",
                "dbConnected", result != null && result == 1,
                "result", result
        );
    }
}
