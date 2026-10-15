package com.duong.eventhub.auth;

import com.duong.eventhub.security.MyUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.hibernate.sql.ast.Clause;
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
    @Value("${jwt.secretkey}")
    private String secretKey;

    private static final long ACCESS_EXPIRATION_MILLIS = 15 * 60 * 1000L;

    private static final long REFRESH_EXPIRATION_MILLIS = 24 * 7 * 60 * 60 * 1000L;



    public String generateAccessToken(MyUserDetails userDetails) {
        Map<String,Object> claims = new HashMap<>();
        Set<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        claims.put("roles", roles);
        claims.put("token_type", "access");
        claims.put("token_version", userDetails.getTokenVersion());
        claims.put("user_id", userDetails.getUserId());
        return buildToken(claims,userDetails,ACCESS_EXPIRATION_MILLIS);
    }


    public String generateRefreshToken(MyUserDetails userDetails) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("token_type", "refresh");
        claims.put("token_version", userDetails.getTokenVersion());
        claims.put("user_id", userDetails.getUserId());
        return buildToken(claims,userDetails,REFRESH_EXPIRATION_MILLIS);
    }

    private String buildToken(Map<String,Object> claims, UserDetails userDetails , long exp){
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                   .id(UUID.randomUUID().toString())
                   .subject(userDetails.getUsername())
                   .claims(claims)
                   .signWith(getKey())
                   .issuedAt(Date.from(issuedAt))
                   .expiration(Date.from(issuedAt.plusMillis(exp)))
                   .compact();

    }

    
    public String extractUsername(String token) {
        return extractAll(token).get();
    }




    public Claims extractAll(String token){
        return Jwts.parser()
                   .verifyWith(getKey())
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }

    public String getEmail(String token){
        return extractAll(token).getSubject();
    }


    private SecretKey getKey(){
        byte[] keyByte = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyByte);
    }


    public Instant extractExpDate(String refreshToken) {
        return extractAll(refreshToken).getExpiration().toInstant();
    }
}
