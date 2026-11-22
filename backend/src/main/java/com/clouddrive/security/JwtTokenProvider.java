package com.clouddrive.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (jwtSecret != null && !jwtSecret.trim().isEmpty()) {
            try {
                byte[] keyBytes = Decoders.BASE64.decode(jwtSecret.trim());
                if (keyBytes.length >= 32) {
                    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
                    log.info("JWT provider initialized with configured Base64 secret key.");
                    return;
                }
            } catch (Exception ignored) {
                byte[] rawBytes = jwtSecret.trim().getBytes(StandardCharsets.UTF_8);
                if (rawBytes.length >= 32) {
                    this.signingKey = Keys.hmacShaKeyFor(rawBytes);
                    log.info("JWT provider initialized with configured raw secret key.");
                    return;
                }
            }
            log.warn("Configured JWT_SECRET is shorter than 256 bits (32 bytes). Generating secure in-memory key.");
        } else {
            log.warn("No JWT_SECRET provided in environment. Generating an ephemeral in-memory 256-bit signing key.");
        }

        // Ephemeral cryptographically secure fallback: unique per application launch
        this.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    private SecretKey getSigningKey() {
        return this.signingKey;
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateTokenFromUsername(userDetails.getUsername());
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
