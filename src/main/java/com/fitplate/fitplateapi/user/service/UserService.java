package com.fitplate.fitplateapi.user.service;

import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User findOrCreateByTossUserKey(String tossUserKey) {
        return userRepository.findByTossUserKey(tossUserKey)
                .orElseGet(() -> userRepository.save(User.create(tossUserKey)));
    }

}
