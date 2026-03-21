package com.safar.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiry-minutes:15}")
    private long expiryMinutes;

    @Value("${jwt.refresh-expiry-days:30}")
    private long refreshExpiryDays;

    private final StringRedisTemplate redis;

    public String generateAccessToken(UUID userId, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMinutes * 60 * 1000))
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(
                "refresh:" + token,
                userId.toString(),
                Duration.ofDays(refreshExpiryDays)
        );
        return token;
    }

    public UUID validateRefreshToken(String token) {
        String userId = redis.opsForValue().get("refresh:" + token);
        if (userId == null) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        return UUID.fromString(userId);
    }

    public void invalidateRefreshToken(String token) {
        redis.delete("refresh:" + token);
    }

    /**
     * Invalidate ALL refresh tokens for a given user (sign out all devices).
     * Scans Redis for refresh:* keys whose value matches the userId.
     */
    public void invalidateAllRefreshTokens(UUID userId) {
        String uid = userId.toString();
        var keys = redis.keys("refresh:*");
        if (keys == null || keys.isEmpty()) return;
        for (String key : keys) {
            String val = redis.opsForValue().get(key);
            if (uid.equals(val)) {
                redis.delete(key);
            }
        }
    }

    public Claims validateAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirySeconds() {
        return expiryMinutes * 60;
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
