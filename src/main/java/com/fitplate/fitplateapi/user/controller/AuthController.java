package com.fitplate.fitplateapi.user.controller;

import com.fitplate.fitplateapi.user.dto.LoginResponse;
import com.fitplate.fitplateapi.user.dto.TossLoginRequest;
import com.fitplate.fitplateapi.user.service.AuthService;
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
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "토스 로그인",
            description = """
                    토스에서 전달받은 인가 코드로 사용자를 인증하고 서비스 API 호출에 사용할 JWT를 발급합니다.

                    처리 과정:
                    - 인가 코드로 토스 액세스 토큰을 발급받습니다.
                    - 토스 사용자 정보를 조회합니다.
                    - 최초 로그인 사용자는 자동으로 등록합니다.
                    - 응답의 accessToken에 서비스 JWT를 반환합니다.

                    이 API는 JWT 인증 없이 호출합니다.
                    """
    )
    @PostMapping("/toss-login")
    public ResponseEntity<LoginResponse> tossLogin(@RequestBody TossLoginRequest request) {
        return ResponseEntity.ok(authService.tossLogin(request));
    }
}
