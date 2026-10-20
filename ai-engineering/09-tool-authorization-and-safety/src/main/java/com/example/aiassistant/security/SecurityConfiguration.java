package com.example.aiassistant.security;

import com.example.aiassistant.AiProperties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {
    @Bean
    JwtDecoder jwtDecoder(
            @Value("${security.issuer}") String issuer,
            @Value("${security.jwk-set-uri}") String jwks,
            @Value("${security.audience}") String audience) {
        JdkClientHttpRequestFactory requests =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        requests.setReadTimeout(Duration.ofSeconds(3));
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(jwks)
                        .restOperations(new RestTemplate(requests))
                        .build();
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        new JwtIssuerValidator(issuer),
                        new JwtTimestampValidator(Duration.ZERO),
                        jwt ->
                                jwt.getSubject() != null
                                                && !jwt.getSubject().isBlank()
                                                && jwt.getExpiresAt() != null
                                                && jwt.getIssuedAt() != null
                                                && !jwt.getIssuedAt().isAfter(Instant.now())
                                                && jwt.getAudience().contains(audience)
                                        ? OAuth2TokenValidatorResult.success()
                                        : OAuth2TokenValidatorResult.failure(
                                                new OAuth2Error("invalid_token"))));
        return token -> {
            try {
                return decoder.decode(token);
            } catch (BadJwtException invalid) {
                throw invalid;
            } catch (JwtException unavailable) {
                throw new IdentityUnavailableException();
            }
        };
    }

    static final class IdentityUnavailableException extends BadJwtException {
        IdentityUnavailableException() {
            super("Login verification is temporarily unavailable");
        }
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> {
                    Object claim = jwt.getClaims().get("permissions");
                    if (!(claim instanceof List<?> permissions)
                            || permissions.stream().anyMatch(value -> !(value instanceof String))) {
                        return List.of();
                    }
                    return permissions.stream()
                            .map(String.class::cast)
                            .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                            .toList();
                });
        AuthenticationEntryPoint entryPoint =
                (request, response, error) -> {
                    boolean unavailable = false;
                    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                        if (cause instanceof IdentityUnavailableException) {
                            unavailable = true;
                            break;
                        }
                    }
                    response.setStatus(unavailable ? 503 : 401);
                    response.setContentType("application/json");
                    response.getWriter()
                            .write(
                                    unavailable
                                            ? "{\"detail\":\"Login verification is temporarily unavailable\"}"
                                            : "{\"detail\":\"Please log in to continue\"}");
                };
        return http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/",
                                                "/index.html",
                                                "/chat.css",
                                                "/chat.js",
                                                "/lesson-config.js",
                                                "/favicon.svg",
                                                "/health",
                                                "/suggestions")
                                        .permitAll()
                                        .requestMatchers("/chat")
                                        .hasAuthority("USERS_WRITE")
                                        .requestMatchers("/me")
                                        .hasAuthority("USERS_READ")
                                        .anyRequest()
                                        .denyAll())
                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                                        .authenticationEntryPoint(entryPoint))
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(entryPoint)
                                        .accessDeniedHandler(
                                                (request, response, error) -> {
                                                    response.setStatus(403);
                                                    response.setContentType("application/json");
                                                    response.getWriter()
                                                            .write(
                                                                    "{\"detail\":\"Permission denied\"}");
                                                }))
                .build();
    }

    @Bean
    CorsConfigurationSource cors(AiProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Request-ID"));
        config.setMaxAge(600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
