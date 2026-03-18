package com.elecbrandy.boilerplate.auth.service;

import com.elecbrandy.boilerplate.auth.domain.dto.LoginRequest;
import com.elecbrandy.boilerplate.auth.domain.dto.TokenInfo;
import com.elecbrandy.boilerplate.auth.domain.entity.RefreshToken;
import com.elecbrandy.boilerplate.auth.jwt.JwtTokenProvider;
import com.elecbrandy.boilerplate.auth.repository.RefreshTokenRepository;
import com.elecbrandy.boilerplate.global.constants.AppConstants;
import com.elecbrandy.boilerplate.global.exception.BusinessException;
import com.elecbrandy.boilerplate.global.response.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetailsService;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;

    private static final int MAX_DEVICE_COUNT = 3;

    @Transactional
    public TokenInfo login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        return this.generateAndSaveToken(authentication);
    }

    private TokenInfo generateAndSaveToken(Authentication authentication) {
        String email = authentication.getName();
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);
        refreshTokenRepository.save(new RefreshToken(email, tokenInfo.getRefreshToken()));
        refreshTokenRepository.deleteOldTokensKeepLatest(email, MAX_DEVICE_COUNT);
        return tokenInfo;
    }

    @Transactional
    public TokenInfo reissue(String requestRefreshToken) {
        if (requestRefreshToken == null) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        Claims claims;
        try {
            claims = jwtTokenProvider.validateAndGetClaims(requestRefreshToken);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.AUTH_EXPIRED_TOKEN);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        String tokenType = claims.get(AppConstants.TOKEN_TYPE_KEY, String.class);
        if (!AppConstants.REFRESH_TOKEN_TYPE.equals(tokenType)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        String email = claims.getSubject();
        Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByKeyAndValue(email, requestRefreshToken);

        if (refreshTokenOptional.isEmpty()) {
            if (!refreshTokenRepository.existsByKey(email)) {
                throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
            }
            log.warn("RTR 공격 의심: {}", email);
            refreshTokenService.deleteAllByKey(email);
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        RefreshToken dbToken = refreshTokenOptional.get();
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, "", userDetails.getAuthorities());

        TokenInfo newToken = jwtTokenProvider.generateToken(authentication);
        dbToken.updateValue(newToken.getRefreshToken());

        return newToken;
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null) return;

        String email;
        boolean isExpired = false;

        try {
            Claims claims = jwtTokenProvider.validateAndGetClaims(refreshToken);
            email = claims.getSubject();
        } catch (ExpiredJwtException e) {
            email = e.getClaims().getSubject();
            isExpired = true;
        } catch (Exception e) {
            return;
        }

        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByValue(refreshToken);

        if (tokenOptional.isPresent()) {
            refreshTokenRepository.delete(tokenOptional.get());
        } else if (!isExpired) {
            log.warn("RTR 공격 의심 (로그아웃 시): {}", email);
            refreshTokenService.deleteAllByKey(email);
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }
}
