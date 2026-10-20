package com.example.aicommerce.commerce.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    JwtDecoder jwtDecoder(
            @Value("${security.issuer}") String issuer,
            @Value("${security.jwk-set-uri}") String jwks,
            @Value("${security.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        jwt ->
                                jwt.getAudience().contains(audience)
                                                && jwt.getSubject() != null
                                                && jwt.getExpiresAt() != null
                                        ? OAuth2TokenValidatorResult.success()
                                        : OAuth2TokenValidatorResult.failure(
                                                new OAuth2Error("invalid_token"))));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> {
                    List<String> permissions = jwt.getClaimAsStringList("permissions");
                    if (permissions == null) {
                        return List.of();
                    }
                    return permissions.stream()
                            .<GrantedAuthority>map(
                                    permission ->
                                            new SimpleGrantedAuthority(
                                                    permission.equals("USER")
                                                                    || permission.equals("ADMIN")
                                                            ? "ROLE_" + permission
                                                            : permission))
                            .toList();
                });
        return http.cors(Customizer.withDefaults())
                // APIs accept explicit bearer tokens, never cookie authentication.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/products",
                                                "/api/products/*",
                                                "/api/categories",
                                                "/images/**")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                                        .authenticationEntryPoint(
                                                (request, response, exception) -> {
                                                    response.setStatus(401);
                                                    response.setContentType("application/json");
                                                    response.getWriter()
                                                            .write(
                                                                    "{\"message\":\"Please log in to continue\"}");
                                                }))
                .exceptionHandling(
                        errors ->
                                errors.accessDeniedHandler(
                                        (request, response, exception) -> {
                                            response.setStatus(403);
                                            response.setContentType("application/json");
                                            response.getWriter()
                                                    .write(
                                                            "{\"message\":\"You do not have permission for this action\"}");
                                        }))
                .build();
    }
}
