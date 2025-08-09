package com.dmclarnon.golf.login_api.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank String username,
    @Email @NotBlank String email,
    @Size(min = 8) String password
) {}
