package com.M198.Majorproject.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-that-is-long-enough-32",
            Duration.ofMinutes(5),
            Duration.ofDays(1));

    @Test
    void accessTokenContainsUserAndAccountClaims() {
        var claims = jwtService.parseAndValidate(
                jwtService.createAccessToken("user-1", "STUDENT"), "access");

        assertEquals("user-1", claims.getSubject());
        assertEquals("STUDENT", claims.get("account_type"));
        assertEquals("access", claims.get("token_type"));
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String refreshToken = jwtService.createRefreshToken("user-1");

        assertThrows(JwtException.class, () -> jwtService.parseAndValidate(refreshToken, "access"));
    }
}