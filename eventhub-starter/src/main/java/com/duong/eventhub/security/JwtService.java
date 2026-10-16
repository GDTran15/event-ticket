package com.duong.eventhub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${app.jwt.access.secret-key}")
    private String accessKey;

    @Value("${app.jwt.refresh.secret-key}")
    private String refreshKey;

    private static final long ACCESS_EXPIRATION_MILLIS =
            15 * 60 * 1000L;

    private static final long REFRESH_EXPIRATION_MILLIS =
            7 * 24 * 60 * 60 * 1000L;


    public String generateAccessToken(MyUserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();

        Set<String> roles = userDetails.getAuthorities()
                                       .stream()
                                       .map(GrantedAuthority::getAuthority)
                                       .collect(Collectors.toSet());

        claims.put("roles", roles);
        claims.put("token_type", "access");
        claims.put("token_version", userDetails.getTokenVersion());
        claims.put("user_id", userDetails.getUserId());

        return buildToken(
                claims,
                userDetails,
                ACCESS_EXPIRATION_MILLIS,
                accessKey
        );
    }


    public String generateRefreshToken(MyUserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("token_type", "refresh");
        claims.put("token_version", userDetails.getTokenVersion());
        claims.put("user_id", userDetails.getUserId());

        return buildToken(
                claims,
                userDetails,
                REFRESH_EXPIRATION_MILLIS,
                refreshKey
        );
    }


    public Claims parseRefreshToken(String token) {

        return Jwts.parser()
                   .verifyWith(getKey(refreshKey))
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }


    private String buildToken(
            Map<String, Object> claims,
            UserDetails userDetails,
            long expiration,
            String key
    ) {

        Instant issuedAt = Instant.now();

        return Jwts.builder()
                   .id(UUID.randomUUID().toString())
                   .subject(userDetails.getUsername())
                   .claims(claims)
                   .issuedAt(Date.from(issuedAt))
                   .expiration(
                           Date.from(
                                   issuedAt.plusMillis(expiration)
                           )
                   )
                   .signWith(getKey(key), Jwts.SIG.HS256)
                   .compact();
    }


    public SecretKey getKey(String secret) {

        byte[] keyBytes =
                Decoders.BASE64.decode(secret);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}