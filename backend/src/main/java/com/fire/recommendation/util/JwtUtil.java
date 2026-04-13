package com.fire.recommendation.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String username, String role) {
        return generateAccessToken(userId, username, role, 0);
    }

    public String generateAccessToken(Long userId, String username, String role, Integer tokenVersion) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("type", "access")
                .claim("tokenVersion", tokenVersion == null ? 0 : tokenVersion)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    public String generateRefreshToken(Long userId, Integer tokenVersion, long refreshExpirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .claim("tokenVersion", tokenVersion == null ? 0 : tokenVersion)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        String sub = parse(token).getSubject();
        return sub == null ? null : Long.parseLong(sub);
    }

    /** JWT claim 中的角色编码，如 USER / AUTHOR / ADMIN */
    public String getRole(String token) {
        Object r = parse(token).get("role");
        return r != null ? r.toString() : null;
    }

    public String getTokenType(String token) {
        Object t = parse(token).get("type");
        return t != null ? t.toString() : "access";
    }

    public Integer getTokenVersion(String token) {
        Object v = parse(token).get("tokenVersion");
        if (v == null) return 0;
        return Integer.parseInt(v.toString());
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            log.warn("JWT 校验失败: {} - token 前20字符: {}", e.getMessage(),
                    token != null && token.length() > 20 ? token.substring(0, 20) + "..." : token);
            return false;
        }
    }
}
