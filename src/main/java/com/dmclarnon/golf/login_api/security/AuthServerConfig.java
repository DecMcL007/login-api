package com.dmclarnon.golf.login_api.security;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

@Configuration
public class AuthServerConfig {

    @Bean
    @Order(1)
    SecurityFilterChain asFilterChain(HttpSecurity http) throws Exception {
        var asConfigurer = new OAuth2AuthorizationServerConfigurer(); // no type param
        var endpoints = asConfigurer.getEndpointsMatcher(); // matches /oauth2/** and /.well-known/**

        http
                .securityMatcher(endpoints)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpoints))
                // replace deprecated .apply(...) with the new style:
                .with(asConfigurer, as -> as.oidc(Customizer.withDefaults()))
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    // ---------- 2) App filter chain (for / auth/register etc.)

    @Bean
    @Order(2)
    SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults());  //login page for testing
         return http.build();
    }

    // ---------- 3) Client (who can request tokens)
    @Bean
    RegisteredClientRepository registeredClientRepository(PasswordEncoder encoder) {
        //Example client for postman / backend to backend
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("golf-client")
                .clientSecret(encoder.encode("golf-secret"))
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                //For authorisation Code Testing, use Postmans callback
                .redirectUri("https://oauth.pstmn.io/v1/callback")
                //scopes to be enforced in resource servers
                .scope("courses.read")
                .scope("courses.write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(39))
                        .build())
                .build();

        //TODO remove 39 mins for production

        return new InMemoryRegisteredClientRepository(client);
        // Later : swpa to JDBC repository to persist clients
    }

    // ----------4) JWK (RSA key) used to sign tokens
    @Bean
    JWKSource<SecurityContext> jwkSource() {
        KeyPair kp = generateRsaKey();
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey priv = (RSAPrivateKey) kp.getPrivate();

        RSAKey rsa = new RSAKey.Builder(pub)
                .privateKey(priv)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsa);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    private static KeyPair generateRsaKey(){
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    //---------- 5) Authorization Server Settings (issuer, etc.)
    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        //Set your issuer; resource servers will validate tokens against this
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8082")
                .build();
    }
}
