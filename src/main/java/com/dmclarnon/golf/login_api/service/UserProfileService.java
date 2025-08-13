package com.dmclarnon.golf.login_api.service;

import com.dmclarnon.golf.login_api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.dmclarnon.golf.login_api.model.AppUser;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public double getHandicapForUser(Long userId) {
        return userRepository.findById(userId)
                .map(AppUser::getHandicap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
