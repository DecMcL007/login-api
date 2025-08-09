package com.dmclarnon.golf.login_api.controller;

import com.dmclarnon.golf.login_api.config.JwtService;
import com.dmclarnon.golf.login_api.dto.requests.LoginRequest;
import com.dmclarnon.golf.login_api.dto.requests.RegisterRequest;
import com.dmclarnon.golf.login_api.dto.responses.AuthResponse;
import com.dmclarnon.golf.login_api.model.AppUser;
import com.dmclarnon.golf.login_api.security.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtService jwt;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AppUser u = userService.register(req);
        String token = jwt.generate(u.getUsername(), u.getRole());
        return ResponseEntity.ok(AuthResponse.bearer(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AppUser u = userService.loadByUsername(req.username());
        if (!userService.verifyPassword(req.password(), u.getPasswordHash()))
            return ResponseEntity.status(401).build();

        String token = jwt.generate(u.getUsername(), u.getRole());
        return ResponseEntity.ok(AuthResponse.bearer(token));
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return Map.of(
                "username", auth.getName(),
                "roles", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        );
    }
}
