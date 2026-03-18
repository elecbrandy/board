package com.elecbrandy.boilerplate.service;

import com.elecbrandy.boilerplate.auth.domain.dto.LoginRequest;
import com.elecbrandy.boilerplate.auth.jwt.JwtTokenProvider;
import com.elecbrandy.boilerplate.auth.repository.RefreshTokenRepository;
import com.elecbrandy.boilerplate.auth.service.AuthService;
import com.elecbrandy.boilerplate.auth.service.RefreshTokenService;
import com.elecbrandy.boilerplate.global.constants.AppConstants;
import com.elecbrandy.boilerplate.global.exception.BusinessException;
import com.elecbrandy.boilerplate.global.response.ErrorCode;
import com.elecbrandy.boilerplate.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // claims.get() 오버로드 mismatch 허용
public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserDetailsService userDetailsService;

    @Test
    @DisplayName("로그인 실패 - 없는 계정이거나 비밀번호가 틀리면 예외가 발생한다")
    void login_fail_bad_credentials() {
        // given
        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@example.com");
        request.setPassword("wrongPassword!");

        given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("Bad credentials"));

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("재발급 실패 - DB에 토큰이 없는데 서명이 유효하면 RTR 탈취로 간주하고 모든 세션을 강제 종료한다")
    void reissue_fail_suspected_rtr_attack() {
        // given
        String stolenToken = "stolen-valid-token";
        String email = "victim@example.com";

        Claims mockClaims = mock(Claims.class);
        given(mockClaims.getSubject()).willReturn(email);

        // 실제 AuthService.reissue()는 claims.get(key, String.class) 오버로드를 호출하므로 맞춰서 stub
        given(mockClaims.get(eq(AppConstants.TOKEN_TYPE_KEY), eq(String.class)))
                .willReturn(AppConstants.REFRESH_TOKEN_TYPE);

        given(jwtTokenProvider.validateAndGetClaims(stolenToken)).willReturn(mockClaims);
        given(refreshTokenRepository.findByKeyAndValue(email, stolenToken)).willReturn(Optional.empty());
        given(refreshTokenRepository.existsByKey(email)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.reissue(stolenToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.AUTH_INVALID_TOKEN.getMessage());

        verify(refreshTokenService).deleteAllByKey(email);
    }

    @Test
    @DisplayName("로그아웃 실패(공격 방어) - 유효한 토큰인데 DB에 없으면 RTR 공격으로 간주하고 세션 파기")
    void logout_rtr_attack_defense() {
        // given
        String abnormalToken = "abnormal-token";
        String email = "victim@example.com";

        Claims mockClaims = mock(Claims.class);
        given(mockClaims.getSubject()).willReturn(email);

        given(jwtTokenProvider.validateAndGetClaims(abnormalToken)).willReturn(mockClaims);
        given(refreshTokenRepository.findByValue(abnormalToken)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.logout(abnormalToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.AUTH_INVALID_TOKEN.getMessage());

        verify(refreshTokenService).deleteAllByKey(email);
    }
}