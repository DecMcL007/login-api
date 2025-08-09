package com.dmclarnon.golf.login_api.dto.requests;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest (
 @NotBlank String username,
 @NotBlank String password
)
{}
