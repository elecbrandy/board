package com.elecbrandy.boilerplate.service;

import com.elecbrandy.boilerplate.auth.service.RefreshTokenService;
import com.elecbrandy.boilerplate.domain.dto.RegisterRequest;
import com.elecbrandy.boilerplate.domain.entity.User;
import com.elecbrandy.boilerplate.global.exception.BusinessException;
import com.elecbrandy.boilerplate.auth.jwt.JwtTokenProvider;
import com.elecbrandy.boilerplate.global.response.ErrorCode;
import com.elecbrandy.boilerplate.auth.repository.RefreshTokenRepository;
import com.elecbrandy.boilerplate.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.verify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
}