package com.elecbrandy.boilerplate.service;

import com.elecbrandy.boilerplate.domain.dto.LoginRequest;
import com.elecbrandy.boilerplate.domain.dto.RegisterRequest;
import com.elecbrandy.boilerplate.domain.dto.RegisterResponse;
import com.elecbrandy.boilerplate.domain.dto.TokenInfo;
import com.elecbrandy.boilerplate.domain.entity.RefreshToken;
import com.elecbrandy.boilerplate.domain.entity.User;
import com.elecbrandy.boilerplate.global.constants.AppConstants;
import com.elecbrandy.boilerplate.global.exception.BusinessException;
import com.elecbrandy.boilerplate.global.jwt.JwtTokenProvider;
import com.elecbrandy.boilerplate.global.response.ErrorCode;
import com.elecbrandy.boilerplate.repository.RefreshTokenRepository;
import com.elecbrandy.boilerplate.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import java.util.List;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService customUserDetailsService;

    @Transactional(readOnly = false)
    public RegisterResponse register(RegisterRequest request) {
        validateDuplicateUser(request);
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = request.toEntity(encodedPassword);
        User savedUser = userRepository.save(user);
        return new RegisterResponse(savedUser);
    }

    @Transactional
    public TokenInfo login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        return this.generateAndSaveToken(authentication);
    }

    private static final int MAX_DEVICE_COUNT = 3;

    private TokenInfo generateAndSaveToken(Authentication authentication) {
        String email = authentication.getName();

        // 새 토큰 먼저 저장
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);
        refreshTokenRepository.save(new RefreshToken(email, tokenInfo.getRefreshToken()));

        // 저장 후 초과분 정리 (MAX_DEVICE_COUNT 만큼만 최신 토큰 유지)
        refreshTokenRepository.deleteOldTokensKeepLatest(email, MAX_DEVICE_COUNT);

        return tokenInfo;
    }


    @Transactional
    public TokenInfo reissue(String requestRefreshToken) {
        if (requestRefreshToken == null) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        // 1. 서명 검증 및 Claims 추출 (단일 파싱으로 통합)
        Claims claims;
        try {
            claims = jwtTokenProvider.validateAndGetClaims(requestRefreshToken);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.AUTH_EXPIRED_TOKEN);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        // 2. 토큰 타입 검증 (REFRESH 타입인지 확인)
        String tokenType = (String) claims.get(AppConstants.TOKEN_TYPE_KEY);
        if (!AppConstants.REFRESH_TOKEN_TYPE.equals(tokenType)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        String email = claims.getSubject();

        // 3. Key+Value로 DB 조회 (이하 기존 로직 동일)
        Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByKeyAndValue(email, requestRefreshToken);

        if (refreshTokenOptional.isEmpty()) {
            if (!refreshTokenRepository.existsByKey(email)) {
                log.info("만료/삭제된 토큰 재발급 시도 (로그아웃 상태) - Email: {}", email);
                throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
            }
            log.warn("RTR 공격 의심(활성 세션 존재하나 만료된 토큰 사용): {}", email);
            refreshTokenService.deleteAllByKey(email);
        }

        // 4. 정상 재발급 (이하 기존과 동일)
        RefreshToken dbToken = refreshTokenOptional.get();

        UserDetails userDetails;
        try {
            userDetails = customUserDetailsService.loadUserByUsername(email);
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
        if (refreshToken == null) {
            log.warn("로그아웃 실패: 토큰이 null입니다.");
            return;
        }

        String email;
        boolean isExpired = false;

        // 1. 토큰에서 Claims 추출 (만료 여부와 무관하게 서명 검증)
        try {
            // 먼저 정상 파싱 시도
            Claims claims = jwtTokenProvider.validateAndGetClaims(refreshToken);
            email = claims.getSubject();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰 - 서명은 유효
            email = e.getClaims().getSubject();
            isExpired = true;
        } catch (BusinessException e) {
            log.warn("로그아웃 실패: 유효하지 않은 토큰 형식입니다. (Cause: {})", e.getMessage());
            return;
        } catch (Exception e) {
            log.error("로그아웃 중 알 수 없는 오류 발생: {}", e.getMessage(), e);
            return;
        }

        // 2. DB 조회
        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByValue(refreshToken);

        if (tokenOptional.isPresent()) {
            // Case A: 정상 로그아웃
            refreshTokenRepository.delete(tokenOptional.get());
            log.info("로그아웃 성공: User Email = {}", email);
        } else if (isExpired) {
            // Case B: 이미 만료된 토큰 -> 단순 재로그인 필요 상황 (공격 아님)
            log.info("만료된 토큰으로 로그아웃 시도 (이미 만료/삭제됨) - Email: {}", email);
            // 아무 처리 없이 종료 (클라이언트 쿠키는 컨트롤러에서 삭제됨)
        } else {
            // Case C: 서명은 유효한데 DB에 없고 만료도 아님 -> RTR 재사용 공격 의심
            log.warn("RTR 공격 의심 (로그아웃 시): 유효한 토큰이나 DB에 없음 - Email: {}", email);
            refreshTokenService.deleteAllByKey(email);
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    private void validateDuplicateUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
    }
}
