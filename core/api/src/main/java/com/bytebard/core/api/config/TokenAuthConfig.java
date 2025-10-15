package com.bytebard.core.api.config;

import com.bytebard.utils.DateUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import javax.crypto.SecretKey;
import java.time.temporal.ChronoUnit;

@Component
public class TokenAuthConfig {

    @Value("${spring.jwt.issuer}")
    private String issuer;

    private SecretKey secretKey;

    public TokenAuthConfig(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public String token(Long subject, Long expirationInHours) {
        var today = DateUtils.now();
        var expiryDate = DateUtils.add(today, expirationInHours, ChronoUnit.HOURS);
        Claims claims = Jwts.claims().subject(String.valueOf(subject))
                .issuedAt(DateUtils.toDate(today))
                .issuer(issuer)
                .expiration(DateUtils.toDate(expiryDate))
                .build();

        return Jwts.builder()
                .claims(claims)
                .expiration(DateUtils.toDate(expiryDate))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUserId(String token) {
        try {
            return getClaims(token).getSubject();
        } catch (JwtException e) {
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN, "Invalid token");
        }
    }

    public String extractSignature(String token) {
        try {
            String[] parts = token.split("\\.");
            return parts.length == 3 ? parts[2] : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
