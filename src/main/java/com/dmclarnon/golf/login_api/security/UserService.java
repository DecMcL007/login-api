package com.dmclarnon.golf.login_api.security;


import com.dmclarnon.golf.login_api.dto.requests.RegisterRequest;
import com.dmclarnon.golf.login_api.model.AppUser;
import com.dmclarnon.golf.login_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository user;
    private final PasswordEncoder encoder;

    public AppUser register(RegisterRequest req) {
        if (user.existsByUsername(req.username())) {
            throw new IllegalArgumentException("username taken!");
        }
        if (user.existsByEmail(req.email())) {
            throw new IllegalArgumentException("email taken !");
        }

        AppUser u = AppUser.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(encoder.encode(req.password1()))
                .role("USER")
                .enabled(true)
                .build();

        return user.save(u);
    }

    public AppUser loadByUsername(String username) {
        return user.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("bad credentials!"));
    }

    public boolean verifyPassword(String raw, String hash) {
        return encoder.matches(raw, hash);
    }
}
