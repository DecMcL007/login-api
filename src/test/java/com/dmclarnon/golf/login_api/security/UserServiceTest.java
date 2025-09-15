package com.dmclarnon.golf.login_api.security;

import com.dmclarnon.golf.login_api.dto.requests.RegisterRequest;
import com.dmclarnon.golf.login_api.model.AppUser;
import com.dmclarnon.golf.login_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void registerThrowsWhenUsernameExists() {
        // arrange
        RegisterRequest req = new RegisterRequest("bob", "bob@example.com", "password123", "password123");
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        // act & assert
        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username taken!");
    }

    @Test
    void registerThrowsWhenEmailExists() {
        // arrange
        RegisterRequest req = new RegisterRequest("bob", "bob@example.com", "password123", "password123");
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        // act & assert
        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email taken !");
    }

    @Test
    void registerEncodesPasswordAndSavesUser() {
        // arrange
        RegisterRequest req = new RegisterRequest("bob", "bob@example.com", "password123", "password123");
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedpw");

        AppUser savedUser = AppUser.builder()
                .username("bob")
                .email("bob@example.com")
                .passwordHash("hashedpw")
                .role("USER")
                .enabled(true)
                .build();

        when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);

        // act
        AppUser result = userService.register(req);

        // assert
        assertThat(result.getUsername()).isEqualTo("bob");
        assertThat(result.getEmail()).isEqualTo("bob@example.com");
        assertThat(result.getPasswordHash()).isEqualTo("hashedpw");

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(AppUser.class));
    }
}