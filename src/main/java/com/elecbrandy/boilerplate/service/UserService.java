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

/**
 * 사용자 계정 관리 및 인증과 관련된 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * <p>
 * - 회원가입 (비밀번호 암호화 및 중복 검증)<br>
 * - 로그인 (Spring Security 기반 인증 및 JWT 발급)<br>
 * - 토큰 재발급 (Refresh Token 검증, RTR 탈취 방어)<br>
 * - 로그아웃 (DB 내 Refresh Token 무효화)
 * </p>
 */
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

    /**
     * 하나의 계정당 유지할 수 있는 최대 Refresh Token(로그인 세션/기기) 개수입니다.
     */
    private static final int MAX_DEVICE_COUNT = 3;

    /**
     * 신규 사용자를 등록합니다.
     * <p>
     * 이메일과 유저네임의 중복 여부를 검증한 뒤, 비밀번호를 단방향 암호화(Bcrypt)하여 DB에 저장합니다.
     * </p>
     *
     * @param request 회원가입 요청 DTO (이메일, 비밀번호, 유저네임)
     * @return 가입 완료된 사용자 정보 DTO
     * @throws BusinessException 이메일 또는 유저네임이 이미 존재할 경우 발생
     */
    @Transactional(readOnly = false)
    public RegisterResponse register(RegisterRequest request) {
        validateDuplicateUser(request);
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = request.toEntity(encodedPassword);
        User savedUser = userRepository.save(user);
        return new RegisterResponse(savedUser);
    }

    /**
     * 사용자의 이메일과 비밀번호를 검증하여 로그인을 수행하고 JWT를 발급합니다.
     * <p>
     * {@link AuthenticationManager}를 통해 자격 증명을 확인한 후,
     * 성공 시 Access Token과 Refresh Token을 생성하여 반환합니다.
     * </p>
     *
     * @param request 로그인 요청 DTO (이메일, 비밀번호)
     * @return 발급된 Token 정보를 담은 DTO
     * @throws org.springframework.security.authentication.BadCredentialsException 비밀번호 불일치 시 발생
     */
    @Transactional
    public TokenInfo login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        return this.generateAndSaveToken(authentication);
    }

    /**
     * 인증 객체를 기반으로 JWT를 생성하고, DB에 Refresh Token을 저장합니다.
     * <p>
     * 다중 기기 로그인을 지원하되 무한정 세션이 늘어나는 것을 방지하기 위해,
     * 새 토큰 저장 후 최신 {@value MAX_DEVICE_COUNT}개의 토큰만 남기고 나머지는 삭제합니다.
     * </p>
     *
     * @param authentication Spring Security 인증 객체
     * @return 생성된 TokenInfo DTO
     */
    private TokenInfo generateAndSaveToken(Authentication authentication) {
        String email = authentication.getName();

        // 새 토큰 먼저 저장
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);
        refreshTokenRepository.save(new RefreshToken(email, tokenInfo.getRefreshToken()));

        // 저장 후 초과분 정리 (MAX_DEVICE_COUNT 만큼만 최신 토큰 유지)
        refreshTokenRepository.deleteOldTokensKeepLatest(email, MAX_DEVICE_COUNT);

        return tokenInfo;
    }

    /**
     * Refresh Token을 사용하여 Access Token과 Refresh Token을 재발급(Rotation)합니다.
     * <p>
     * 만약 클라이언트가 보낸 토큰이 유효한 서명을 가졌으나 DB에 존재하지 않는다면,
     * 해당 토큰이 이미 사용되었거나 탈취되어 공격자가 사용했을 가능성이 높다고 판단하여
     * 해당 사용자의 모든 Refresh Token을 강제 삭제(강제 로그아웃) 처리합니다.
     * </p>
     *
     * @param requestRefreshToken 클라이언트로부터 받은 Refresh Token 문자열
     * @return 새로 갱신된 TokenInfo DTO
     * @throws BusinessException 토큰이 만료되었거나, 유효하지 않거나, 탈취가 의심될 경우 발생
     */
    @Transactional
    public TokenInfo reissue(String requestRefreshToken) {
        if (requestRefreshToken == null) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        // 1. 서명 검증 및 Claims 추출
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

        // 4. 정상 재발급
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

    /**
     * 시스템에서 사용자를 로그아웃 처리하고 DB의 Refresh Token을 삭제합니다.
     * <p>
     * 토큰이 만료된 상태에서의 로그아웃 요청은 자연스러운 현상으로 간주하여 무시합니다.
     * 하지만 서명은 유효한데 DB에 토큰이 없는 경우, 비정상적인 접근(RTR 재사용 공격)으로
     * 간주하여 모든 세션을 파기합니다.
     * </p>
     *
     * @param refreshToken 클라이언트로부터 받은 Refresh Token 문자열
     * @throws BusinessException RTR 공격이 의심될 경우 발생
     */
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

    /**
     * 회원가입 시 이메일과 유저네임의 중복을 검증합니다.
     *
     * @param request 중복을 확인할 회원가입 요청 DTO
     * @throws BusinessException 중복된 이메일이나 유저네임이 존재할 경우 발생
     */
    private void validateDuplicateUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
    }
}
