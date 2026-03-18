package com.elecbrandy.boilerplate.service;

import com.elecbrandy.boilerplate.domain.dto.LoginRequest;
import com.elecbrandy.boilerplate.domain.dto.RegisterRequest;
import com.elecbrandy.boilerplate.domain.entity.User;
import com.elecbrandy.boilerplate.global.exception.BusinessException;
import com.elecbrandy.boilerplate.global.jwt.JwtTokenProvider;
import com.elecbrandy.boilerplate.global.response.ErrorCode;
import com.elecbrandy.boilerplate.repository.RefreshTokenRepository;
import com.elecbrandy.boilerplate.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.jsonwebtoken.Claims;
import com.elecbrandy.boilerplate.global.constants.AppConstants;
import com.elecbrandy.boilerplate.service.RefreshTokenService;
import com.elecbrandy.boilerplate.service.CustomUserDetailsService;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService; // 테스트할 실제 객체 (가짜 객체들이 주입됨)

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("가입 실패 - 이미 존재하는 이메일이면 예외가 발생한다")
    void register_fail_duplicate_email() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicate@example.com");
        request.setUsername("테스터");
        request.setPassword("Password123!");

        // DB에 이미 해당 이메일이 있다고 가정 (Mocking)
        given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

        // when & then: BusinessException이 터지고, 에러 코드가 DUPLICATE_EMAIL인지 검증
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.DUPLICATE_EMAIL.getMessage())
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("가입 실패 - 이미 존재하는 닉네임이면 예외가 발생한다")
    void register_fail_duplicate_username() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setUsername("duplicate_name");
        request.setPassword("Password123!");

        // 이메일은 없고, 닉네임은 있다고 가정
        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.existsByUsername(request.getUsername())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.DUPLICATE_USERNAME.getMessage());
    }

    @Test
    @DisplayName("가입 성공 - 비밀번호가 정상적으로 암호화되어 저장된다")
    void register_success_password_is_encoded() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setUsername("테스터");
        request.setPassword("rawPassword123!");

        given(userRepository.existsByEmail(any())).willReturn(false);
        given(userRepository.existsByUsername(any())).willReturn(false);

        // 인코더가 원본 비밀번호를 받으면 암호화된 문자열을 반환하도록 설정
        given(passwordEncoder.encode("rawPassword123!")).willReturn("encodedPassword!@#");

        // save 시 더미 User 객체 반환
        given(userRepository.save(any(User.class))).willReturn(
                User.builder().email("test@example.com").username("테스터").password("encodedPassword!@#").build()
        );

        // when
        userService.register(request);

        // then
        // UserRepository의 save 메서드에 전달된 User 객체를 가로채서(Capture) 검증
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword!@#"); // 원본이 아닌 암호화된 값인지 확인
        assertThat(savedUser.getPassword()).isNotEqualTo("rawPassword123!");
    }

    @Test
    @DisplayName("로그인 실패 - 없는 계정이거나 비밀번호가 틀리면 예외가 발생한다")
    void login_fail_bad_credentials() {
        // given
        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@example.com");
        request.setPassword("wrongPassword!");

        // Spring Security의 AuthenticationManager는 자격증명 실패 시 BadCredentialsException을 던짐
        given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("Bad credentials"));

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("재발급 실패 - DB에 토큰이 없는데 서명이 유효하면 RTR 탈취로 간주하고 모든 세션을 강제 종료한다")
    void reissue_fail_suspected_rtr_attack() {
        // given
        String stolenToken = "stolen-valid-token";
        String email = "victim@example.com";

        // JJWT 버전 의존성 없이 Claims 자체를 Mocking 처리
        Claims mockClaims = mock(Claims.class);
        given(mockClaims.getSubject()).willReturn(email);
        given(mockClaims.get(AppConstants.TOKEN_TYPE_KEY, String.class)).willReturn(AppConstants.REFRESH_TOKEN_TYPE);

        // 서명 검증은 통과했다고 가정
        given(jwtTokenProvider.validateAndGetClaims(stolenToken)).willReturn(mockClaims);

        // 하지만 DB에는 해당 토큰이 없음 (이미 한 번 사용되었거나 로그아웃됨)
        given(refreshTokenRepository.findByKeyAndValue(email, stolenToken)).willReturn(Optional.empty());

        // 활성화된 다른 세션(기기)이 존재한다고 가정
        given(refreshTokenRepository.existsByKey(email)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.reissue(stolenToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.AUTH_INVALID_TOKEN.getMessage());

        // 탈취 방어를 위해 해당 유저의 모든 토큰을 삭제했는지 검증
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

        // DB에 없음
        given(refreshTokenRepository.findByValue(abnormalToken)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.logout(abnormalToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.AUTH_INVALID_TOKEN.getMessage());

        verify(refreshTokenService).deleteAllByKey(email);
    }

}