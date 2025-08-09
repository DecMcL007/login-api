package com.dmclarnon.golf.login_api.security;

import com.dmclarnon.golf.login_api.model.AppUser;
import com.dmclarnon.golf.login_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class UserAuthConfig {

    private final UserRepository userRepository;

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            AppUser u = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            // Map your "USER"/"ADMIN" to Spring roles
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole()));
            return User.withUsername(u.getUsername())
                    .password(u.getPasswordHash())   // already BCrypt-hashed in DB
                    .authorities(authorities)
                    .accountLocked(!u.isEnabled())
                    .build();
        };
    }
}