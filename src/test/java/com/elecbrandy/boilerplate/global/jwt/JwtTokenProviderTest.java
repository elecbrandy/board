package com.elecbrandy.boilerplate.global.jwt;

import com.elecbrandy.boilerplate.auth.domain.dto.TokenInfo;
import com.elecbrandy.boilerplate.auth.jwt.JwtTokenProvider;
import com.elecbrandy.boilerplate.global.constants.AppConstants;
import com.elecbrandy.boilerplate.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    // 테스트용 임시 시크릿 키 (Base64 인코딩된 256bit 이상 문자열)
    private final String secretKey = "c2lsdmVybmluZS10ZWNoLXNwcmluZy1ib290LWp3dC10dXRvcmlhbC1zZWNyZXQtc2lsdmVybmluZS10ZWNoLXNwcmluZy1ib290LWp3dC10dXRvcmlhbC1zZWNyZXQK";
    private final long accessTokenExpiration = 1000 * 60 * 30; // 30분
    private final long refreshTokenExpiration = 1000 * 60 * 60 * 24; // 24시간

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secretKey, accessTokenExpiration, refreshTokenExpiration);
    }

    @Test
    @DisplayName("토큰 발급 - Authentication 객체로 Access, Refresh 토큰을 발급한다")
    void generateToken_success() {
        // given
        User principal = new User("test@example.com", "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());

        // when
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // then
        assertThat(tokenInfo).isNotNull();
        assertThat(tokenInfo.getGrantType()).isEqualTo(AppConstants.BEARER_PREFIX.trim());
        assertThat(tokenInfo.getAccessToken()).isNotBlank();
        assertThat(tokenInfo.getRefreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("토큰 검증 - Access Token에서 클레임을 정상적으로 추출한다")
    void getAuthentication_success() {
        // given
        User principal = new User("test@example.com", "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // when
        Authentication extractedAuth = jwtTokenProvider.getAuthentication(tokenInfo.getAccessToken());

        // then
        assertThat(extractedAuth.getName()).isEqualTo("test@example.com");
        assertThat(extractedAuth.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("토큰 타입 검증 - Refresh Token으로 Authentication 추출 시 예외가 발생한다")
    void getAuthentication_fail_with_refresh_token() {
        // given
        User principal = new User("test@example.com", "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // when & then
        assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(tokenInfo.getRefreshToken()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 토큰입니다.");
    }
}