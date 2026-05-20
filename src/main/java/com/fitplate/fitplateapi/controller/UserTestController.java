package com.fitplate.fitplateapi.controller;

import com.fitplate.fitplateapi.domain.User;
import com.fitplate.fitplateapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/users")
public class UserTestController {

    private final UserRepository userRepository;

    @PostMapping("/mock")
    public Map<String, Object> createMockUser() {
        User user = userRepository
                .findByTossUserKey("MOCK_USER_001")
                .orElseGet(() -> userRepository.save(
                        new User("MOCK_USER_001", "test_user")
                ));

        return Map.of(
                "userId", user.getUserId(),
                "tossUserKey", user.getTossUserKey(),
                "nickname", user.getNickname()
        );
    }

    @GetMapping("/mock")
    public Map<String, Object> getMockUser() {
        User user = userRepository
                .findByTossUserKey("MOCK_USER_001")
                .orElseThrow(() -> new IllegalStateException("mock user가 없습니다."));

        return Map.of(
                "userId", user.getUserId(),
                "tossUserKey", user.getTossUserKey(),
                "nickname", user.getNickname()
        );
    }
}