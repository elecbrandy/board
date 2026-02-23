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

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void doFilterInternal(@NonNull HttpServletRequest request,
                                 @NonNull HttpServletResponse response,
                                 @NonNull FilterChain chain) throws ServletException, IOException {

        String token = resolveToken(request);

        try {
            if (token != null) {
                // [수정] validateToken()이 이제 void이며 실패 시 예외를 던짐
                jwtTokenProvider.validateToken(token);

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

        chain.doFilter(request, response);
    }

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
