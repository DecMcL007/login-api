package com.dmclarnon.golf.login_api.controller;


import com.dmclarnon.golf.login_api.config.JwtService;
import com.dmclarnon.golf.login_api.dto.requests.LoginRequest;
import com.dmclarnon.golf.login_api.dto.responses.HandicapResponse;
import com.dmclarnon.golf.login_api.security.UserService;
import com.dmclarnon.golf.login_api.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }
    @GetMapping("/{userId}/handicap")
    public ResponseEntity<HandicapResponse> getUserHandicap (@PathVariable Long userId){
        double handicap = userProfileService.getHandicapForUser(userId);
        return ResponseEntity.ok(new HandicapResponse(userId, handicap));
    }
}
