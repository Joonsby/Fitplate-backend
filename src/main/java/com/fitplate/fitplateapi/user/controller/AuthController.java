package com.fitplate.fitplateapi.user.controller;

import com.fitplate.fitplateapi.user.dto.LoginResponse;
import com.fitplate.fitplateapi.user.dto.TossLoginRequest;
import com.fitplate.fitplateapi.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/toss-login")
    public ResponseEntity<LoginResponse> tossLogin(@RequestBody TossLoginRequest request) {
        return ResponseEntity.ok(authService.tossLogin(request));
    }
}