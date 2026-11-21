package com.hrflow.security;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** JWT mang userId và token_version; tăng token_version trong DB sẽ thu hồi mọi token cũ. */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long kioskExpirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms:3600000}") long expirationMs,
                      @Value("${app.jwt.kiosk-expiration-ms:43200000}") long kioskExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expirationMs = expirationMs;
        this.kioskExpirationMs = kioskExpirationMs;
    }

    public record Issued(String token, Instant expiresAt) {
    }

    public Issued generate(long userId, String email, int tokenVersion) {
        return generate(userId, email, tokenVersion, false);
    }

    /**
     * Tài khoản thiết bị KIOSK (máy chấm công tại quầy) được phiên dài hơn (mặc định 12 giờ, NFR-01)
     * để không bị đăng xuất giữa ca; vẫn bị thu hồi ngay khi khóa tài khoản (token_version).
     */
    public Issued generate(long userId, String email, int tokenVersion, boolean kiosk) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (kiosk ? kioskExpirationMs : expirationMs));
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("tv", tokenVersion)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
        return new Issued(token, expiry.toInstant());
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
}
