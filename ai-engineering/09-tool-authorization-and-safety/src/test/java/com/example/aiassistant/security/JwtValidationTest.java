package com.example.aiassistant.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtClaimsSet.Builder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

class JwtValidationTest {
    @Test
    void verifiesSignatureIssuerAudienceAndExpiry() throws Exception {
        RSAKey key = new RSAKeyGenerator(2048).keyID("synthetic-key").generate();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/jwks",
                exchange -> {
                    byte[] body =
                            new JWKSet(key.toPublicJWK())
                                    .toString()
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream stream = exchange.getResponseBody()) {
                        stream.write(body);
                    }
                });
        server.start();
        try {
            JwtDecoder decoder =
                    new SecurityConfiguration()
                            .jwtDecoder(
                                    "https://issuer.test",
                                    "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks",
                                    "ai-commerce-api");
            Instant future = Instant.now().plusSeconds(300);
            assertEquals(
                    "alice",
                    decoder.decode(token(key, "https://issuer.test", "ai-commerce-api", future))
                            .getSubject());
            assertThrows(
                    JwtException.class,
                    () ->
                            decoder.decode(
                                    token(key, "https://wrong.test", "ai-commerce-api", future)));
            assertThrows(
                    JwtException.class,
                    () ->
                            decoder.decode(
                                    token(key, "https://issuer.test", "wrong-audience", future)));
            assertThrows(
                    JwtException.class,
                    () ->
                            decoder.decode(
                                    token(
                                            key,
                                            "https://issuer.test",
                                            "ai-commerce-api",
                                            Instant.now().minusSeconds(120))));
            assertThrows(
                    JwtException.class,
                    () ->
                            decoder.decode(
                                    token(
                                            key,
                                            "https://issuer.test",
                                            "ai-commerce-api",
                                            Instant.now().minusSeconds(1))));
            RSAKey forged = new RSAKeyGenerator(2048).keyID("synthetic-key").generate();
            assertThrows(
                    JwtException.class,
                    () ->
                            decoder.decode(
                                    token(
                                            forged,
                                            "https://issuer.test",
                                            "ai-commerce-api",
                                            future)));
            for (Instant issued : new Instant[] {null, Instant.now().plusSeconds(120)}) {
                NimbusJwtEncoder encoder =
                        new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
                Builder claims =
                        JwtClaimsSet.builder()
                                .issuer("https://issuer.test")
                                .subject("alice")
                                .audience(List.of("ai-commerce-api"))
                                .expiresAt(future);
                if (issued != null) {
                    claims.issuedAt(issued);
                }
                String invalid =
                        encoder.encode(
                                        JwtEncoderParameters.from(
                                                JwsHeader.with(SignatureAlgorithm.RS256)
                                                        .keyId("synthetic-key")
                                                        .build(),
                                                claims.build()))
                                .getTokenValue();
                assertThrows(JwtException.class, () -> decoder.decode(invalid));
            }
            JwtDecoder unavailable =
                    new SecurityConfiguration()
                            .jwtDecoder(
                                    "https://issuer.test",
                                    "http://127.0.0.1:"
                                            + server.getAddress().getPort()
                                            + "/missing-jwks",
                                    "ai-commerce-api");
            assertThrows(
                    SecurityConfiguration.IdentityUnavailableException.class,
                    () ->
                            unavailable.decode(
                                    token(key, "https://issuer.test", "ai-commerce-api", future)));
        } finally {
            server.stop(0);
        }
    }

    private String token(RSAKey key, String issuer, String audience, Instant expiration) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject("alice")
                        .audience(List.of(audience))
                        .issuedAt(Instant.now().minusSeconds(300))
                        .expiresAt(expiration)
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(SignatureAlgorithm.RS256)
                                        .keyId("synthetic-key")
                                        .build(),
                                claims))
                .getTokenValue();
    }
}
