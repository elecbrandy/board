package com.elecbrandy.boilerplate.global.jwt;

import com.elecbrandy.boilerplate.domain.dto.TokenInfo;
import com.elecbrandy.boilerplate.global.constants.AppConstants;
import com.elecbrandy.boilerplate.global.exception.BusinessException;
import com.elecbrandy.boilerplate.global.response.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Collections;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;

    @Getter
    private final long accessTokenExpiration;

    @Getter
    private final long refreshTokenExpiration;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
                            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public TokenInfo generateToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + accessTokenExpiration);

        String accessToken = Jwts.builder()
                .subject(authentication.getName())
                .claim(AppConstants.AUTHORITIES_KEY, authorities)
                .claim(AppConstants.TOKEN_TYPE_KEY, AppConstants.ACCESS_TOKEN_TYPE) // TYPE: ACCESS
                .expiration(accessTokenExpiresIn)
                .signWith(key)
                .compact();

        String refreshToken = Jwts.builder()
                .subject(authentication.getName())
                .claim(AppConstants.TOKEN_TYPE_KEY, AppConstants.REFRESH_TOKEN_TYPE) // TYPE: REFRESH
                .expiration(new Date(now + refreshTokenExpiration))
                .signWith(key)
                .compact();

        return TokenInfo.builder()
                .grantType(AppConstants.BEARER_PREFIX.trim())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        // 토큰 타입 검증 (Access Token이 맞는지 확인)
        String tokenType = claims.get(AppConstants.TOKEN_TYPE_KEY, String.class);
        if (!AppConstants.ACCESS_TOKEN_TYPE.equals(tokenType)) {
            // Refresh Token으로 접근 시도를 막음
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        if (claims.get(AppConstants.AUTHORITIES_KEY) == null ||
                claims.get(AppConstants.AUTHORITIES_KEY).toString().trim().isEmpty()) {
            User principal = new User(claims.getSubject(), "", Collections.emptyList());
            return new UsernamePasswordAuthenticationToken(principal, "", Collections.emptyList());
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AppConstants.AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token); // 만료 여부 및 서명 확인
            String tokenType = (String) claims.get(AppConstants.TOKEN_TYPE_KEY);
            return AppConstants.REFRESH_TOKEN_TYPE.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    public void validateToken(String token) {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    public Claims validateAndGetClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Claims getClaimsIgnoringExpiration(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(accessToken).getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}