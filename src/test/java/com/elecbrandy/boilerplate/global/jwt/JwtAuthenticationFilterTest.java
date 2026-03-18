package com.elecbrandy.boilerplate.global.jwt;

import com.elecbrandy.boilerplate.global.constants.AppConstants;
import com.elecbrandy.boilerplate.global.response.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext(); // 컨텍스트 초기화
    }

    @Test
    @DisplayName("유효한 헤더 토큰이 들어오면 SecurityContext에 Authentication을 세팅한다")
    void doFilterInternal_validToken_setsAuthentication() throws Exception {
        // given
        String token = "valid-token";
        request.addHeader(AppConstants.AUTHORIZATION_HEADER, AppConstants.BEARER_PREFIX + token);

        Authentication mockAuth = mock(Authentication.class);
        given(jwtTokenProvider.getAuthentication(token)).willReturn(mockAuth);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(mockAuth);
        verify(filterChain).doFilter(request, response); // 다음 필터로 잘 넘어갔는지 확인
    }

    @Test
    @DisplayName("만료된 토큰이 들어오면 request에 EXPIRED_TOKEN 에러 코드를 세팅한다")
    void doFilterInternal_expiredToken_setsExceptionAttribute() throws Exception {
        // given
        String token = "expired-token";
        request.addHeader(AppConstants.AUTHORIZATION_HEADER, AppConstants.BEARER_PREFIX + token);

        willThrow(ExpiredJwtException.class).given(jwtTokenProvider).validateToken(anyString());

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute("exception")).isEqualTo(ErrorCode.AUTH_EXPIRED_TOKEN);
        verify(filterChain).doFilter(request, response);
    }
}