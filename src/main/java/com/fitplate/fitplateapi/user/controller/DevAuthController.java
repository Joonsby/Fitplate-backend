package com.fitplate.fitplateapi.user.controller;

import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.dto.LoginResponse;
import com.fitplate.fitplateapi.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(
        name = "인증",
        description = "토스 로그인 및 서비스 JWT 발급 기능"
)
public class DevAuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(
            summary = "개발용 로그인",
            description = """
                    로컬 개발과 API 테스트를 위해 고정 사용자(MOCK_USER_001)의 서비스 JWT를 발급합니다.

                    사용자가 없으면 자동으로 생성하며, 요청 본문과 JWT 인증은 필요하지 않습니다.
                    운영 환경에서는 노출하지 않도록 주의해야 합니다.
                    """
    )
    @PostMapping("/dev-login")
    public ResponseEntity<LoginResponse> devLogin() {
        User user = userService.findOrCreateByTossUserKey("MOCK_USER_001");
        String token = jwtTokenProvider.createToken(user.getTossUserKey());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
