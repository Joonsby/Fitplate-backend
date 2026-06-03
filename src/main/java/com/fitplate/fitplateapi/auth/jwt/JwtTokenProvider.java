package com.fitplate.fitplateapi.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String SECRET =
            "fitplate-local-dev-secret-key-must-be-at-least-32-bytes";

    private final SecretKey secretKey =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final long expirationMillis = Duration.ofDays(7).toMillis();

    public String createToken(String tossUserKey) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(tossUserKey)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(secretKey)
                .compact();
    }

    public String getTossUserKey(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 헤더가 올바르지 않습니다.");
        }

        return authorizationHeader.substring(7);
    }
}