package com.fitplate.fitplateapi.user.controller;

import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.dto.LoginResponse;
import com.fitplate.fitplateapi.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class DevAuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/dev-login")
    public ResponseEntity<LoginResponse> devLogin() {
        User user = userService.findOrCreateByTossUserKey("MOCK_USER_001");
        String token = jwtTokenProvider.createToken(user.getTossUserKey());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}