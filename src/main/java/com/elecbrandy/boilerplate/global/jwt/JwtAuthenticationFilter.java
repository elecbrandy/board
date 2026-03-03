package com.elecbrandy.boilerplate.global.jwt;

import com.elecbrandy.boilerplate.global.constants.AppConstants;
import com.elecbrandy.boilerplate.global.exception.BusinessException;
import com.elecbrandy.boilerplate.global.response.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;


/**
 * JWT 인증을 처리하는 커스텀 보안 필터 클래스입니다.
 * <p>
 * Spring Security의 {@link OncePerRequestFilter}를 상속받아 HTTP 요청당 한 번씩 실행됩니다.
 * 들어오는 HTTP 요청의 헤더 또는 쿠키에서 JWT(Access Token)를 추출하고 유효성을 검증합니다.
 * 토큰이 유효한 경우 {@link SecurityContextHolder}에 인증(Authentication) 객체를 저장하여 이후의 보안 로직을 통과시킵니다.
 * <p>
 * 토큰 검증 과정에서 예외가 발생하면, 해당 오류에 맞는 {@link ErrorCode}를 {@code request}의 속성(attribute)으로 저장하여
 * {@link JwtAuthenticationEntryPoint}에서 클라이언트에게 적절한 에러 응답을 내보낼 수 있도록 합니다.
 * * @author elecbrandy
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * HTTP 요청을 인터셉트하여 JWT 인증 로직을 수행합니다.
     *
     * @param request  현재 HTTP 요청 객체
     * @param response 현재 HTTP 응답 객체
     * @param chain    다음 필터로 제어를 넘기기 위한 필터 체인
     * @throws ServletException 서블릿 예외 발생 시
     * @throws IOException      입출력 처리 중 예외 발생 시
     */
    @Override
    public void doFilterInternal(@NonNull HttpServletRequest request,
                                 @NonNull HttpServletResponse response,
                                 @NonNull FilterChain chain) throws ServletException, IOException {

        String token = resolveToken(request);

        try {
            if (token != null) {
                jwtTokenProvider.validateToken(token); // 실패 시 예외를 던짐

                // 검증 통과 시 Authentication 설정
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            request.setAttribute("exception", ErrorCode.AUTH_EXPIRED_TOKEN);
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException | io.jsonwebtoken.security.SecurityException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            request.setAttribute("exception", ErrorCode.AUTH_INVALID_TOKEN);
        } catch (BusinessException e) {
            // Refresh Token으로 API 접근 등 명시적 JWT 관련 비즈니스 예외
            log.warn("JWT Business Error: {}", e.getMessage());
            request.setAttribute("exception", e.getErrorCode());
        } catch (Exception e) {
            log.error("JWT Filter Internal Error: {}", e.getMessage());
            request.setAttribute("exception", ErrorCode.INTERNAL_SERVER_ERROR);
        }

        chain.doFilter(request, response); // 다음 필터로 이동
    }

    /**
     * HTTP 요청에서 JWT 토큰 문자열을 추출합니다.
     * <p>
     * 1. HTTP 헤더({@code Authorization})에서 {@code Bearer } 접두사로 시작하는 토큰 추출<br>
     * 2. 헤더에 토큰이 없을 경우, HTTP 쿠키에서 {@code accessToken}이라는 이름의 쿠키 값 추출
     * </p>
     *
     * @param request 현재 HTTP 요청 객체
     * @return 추출된 JWT 문자열 (존재하지 않을 경우 {@code null} 반환)
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AppConstants.AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(AppConstants.BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(cookie -> AppConstants.ACCESS_TOKEN.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
