package com.M198.Majorproject.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";

    private final SecretKey signingKey;
    private final Duration accessTokenLifetime;
    private final Duration refreshTokenLifetime;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-lifetime:15m}") Duration accessTokenLifetime,
            @Value("${app.jwt.refresh-token-lifetime:7d}") Duration refreshTokenLifetime) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("app.jwt.secret must be at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenLifetime = accessTokenLifetime;
        this.refreshTokenLifetime = refreshTokenLifetime;
    }

    public String createAccessToken(String userId, String accountType) {
        return createToken(userId, accountType, ACCESS_TOKEN, accessTokenLifetime);
    }

    public String createRefreshToken(String userId) {
        return createToken(userId, null, REFRESH_TOKEN, refreshTokenLifetime);
    }

    public Instant refreshTokenExpiresAt() {
        return Instant.now().plus(refreshTokenLifetime);
    }

    public Claims parseAndValidate(String token, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new JwtException("Unexpected token type");
        }
        return claims;
    }

    private String createToken(String userId, String accountType, String tokenType, Duration lifetime) {
        Instant issuedAt = Instant.now();
        var builder = Jwts.builder()
                .subject(userId)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(lifetime)))
                .signWith(signingKey);
        if (accountType != null) {
            builder.claim("account_type", accountType);
        }
        return builder.compact();
    }
}
