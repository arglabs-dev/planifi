package com.planifi.backend.application;

import com.planifi.backend.config.JwtProperties;
import com.planifi.backend.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String EMAIL_CLAIM = "email";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public JwtToken issueToken(User user) {
        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plusMinutes(jwtProperties.getExpirationMinutes());
        String token = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(user.getId().toString())
                .claim(EMAIL_CLAIM, user.getEmail())
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt.toInstant()))
                .signWith(secretKey)
                .compact();
        return new JwtToken(token, expiresAt);
    }

    public JwtUserClaims parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get(EMAIL_CLAIM, String.class);
        return new JwtUserClaims(userId, email);
    }
}
