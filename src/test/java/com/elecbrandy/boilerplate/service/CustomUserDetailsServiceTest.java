package com.elecbrandy.boilerplate.service;

import com.elecbrandy.boilerplate.domain.entity.User;
import com.elecbrandy.boilerplate.domain.enums.Role;
import com.elecbrandy.boilerplate.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("이메일로 사용자를 찾아 UserDetails 객체를 반환한다")
    void loadUserByUsername_success() {
        // given
        String email = "test@example.com";
        User user = User.builder()
                .email(email)
                .password("encodedPassword")
                .username("테스터")
                .role(Role.USER)
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // then
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회 시 예외가 발생한다")
    void loadUserByUsername_fail_not_found() {
        // given
        String email = "unknown@example.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("해당하는 유저를 찾을 수 없습니다.");
    }
}