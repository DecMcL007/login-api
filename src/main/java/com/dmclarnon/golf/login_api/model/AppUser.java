package com.dmclarnon.golf.login_api.model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role; //Intention is to have two roles 'ADMIN' and 'USER'

    @Column(nullable = false)
    private boolean enabled = true;
}
