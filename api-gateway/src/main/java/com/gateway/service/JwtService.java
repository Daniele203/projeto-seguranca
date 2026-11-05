package com.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.issuer}") String issuer,
                      @Value("${app.jwt.expiration-ms:900000}") long expirationMs) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET precisa ter pelo menos 32 bytes UTF-8");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, List<String> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .setSubject(username)
            .setIssuer(issuer)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(now + expirationMs))
            .claim("roles", roles)
            .signWith(key)
            .compact();
    }

    public Claims validateToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .requireIssuer(issuer)
            .build()
            .parseClaimsJws(token)
            .getBody();
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new IllegalArgumentException("JWT sem subject");
        }
        if (claims.getExpiration() == null) {
            throw new IllegalArgumentException("JWT sem expiração");
        }
        return claims;
    }
}
