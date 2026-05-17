package com.briefy.domain.user.service;

import com.briefy.domain.user.entity.User;
import com.briefy.domain.user.repository.UserRepository;
import com.briefy.global.error.BriefyErrorCode;
import com.briefy.global.error.BriefyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BriefyException(BriefyErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public User updateName(UUID userId, String name) {
        User user = findById(userId);
        user.updateName(name);
        return user;
    }

    @Transactional
    public void delete(UUID userId) {
        userRepository.delete(findById(userId));
    }
}
