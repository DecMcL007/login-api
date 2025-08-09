package com.dmclarnon.golf.login_api.dto.responses;

public record AuthResponse(String accessToken, String tokenType){
    public static AuthResponse bearer(String token){
        return new AuthResponse(token, "bearer");
    }
}
