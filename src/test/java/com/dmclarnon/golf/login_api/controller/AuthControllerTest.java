package com.dmclarnon.golf.login_api.controller;

import com.dmclarnon.golf.login_api.config.JwtService;
import com.dmclarnon.golf.login_api.dto.requests.RegisterRequest;
import com.dmclarnon.golf.login_api.dto.responses.AuthResponse;
import com.dmclarnon.golf.login_api.model.AppUser;
import com.dmclarnon.golf.login_api.security.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final UserService userService = Mockito.mock(UserService.class);
    private final JwtService jwtService = Mockito.mock(JwtService.class);
    private final AuthController controller = new AuthController(userService, jwtService);

    @Test
    void registerReturnsBearerToken() {
        // arrange
        RegisterRequest req = new RegisterRequest("testuser", "pw123", "pw123", "pw123");
        AppUser fakeUser = new AppUser();
        fakeUser.setUsername("testuser");
        fakeUser.setRole("USER");

        when(userService.register(any(RegisterRequest.class))).thenReturn(fakeUser);
        when(jwtService.generate("testuser", "USER")).thenReturn("mocked-jwt");

        // act
        ResponseEntity<AuthResponse> response = controller.register(req);

        // assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().accessToken()).isEqualTo("Bearer mocked-jwt");
        assertThat(response.getBody().tokenType()).isEqualTo("bearer");
    }
}