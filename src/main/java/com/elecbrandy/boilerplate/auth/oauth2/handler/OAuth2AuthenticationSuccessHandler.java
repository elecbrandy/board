package com.elecbrandy.boilerplate.auth.oauth2.handler;

import com.elecbrandy.boilerplate.auth.domain.dto.TokenInfo;
import com.elecbrandy.boilerplate.auth.domain.entity.RefreshToken;
import com.elecbrandy.boilerplate.auth.jwt.JwtTokenProvider;
import com.elecbrandy.boilerplate.auth.repository.RefreshTokenRepository;
import com.elecbrandy.boilerplate.global.constants.AppConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Google OAuth2 인증 성공 후 처리를 담당하는 핸들러입니다.
 * <p>
 * 인증이 완료된 {@link Authentication} 객체를 받아 기존 {@link JwtTokenProvider}로
 * Access/Refresh 토큰을 발급하고 HttpOnly 쿠키에 담아 프론트엔드로 리다이렉트합니다.
 * <br>
 * 토큰 발급 및 저장 로직은 기존 JWT 인프라를 그대로 재사용합니다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final int MAX_DEVICE_COUNT = 3;

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.cookie-secure}")
    private boolean cookieSecure;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/callback}")
    private String redirectUri;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String email = authentication.getName();
        log.info("OAuth2 로그인 성공 - email: {}", email);

        // 1. 기존 JwtTokenProvider로 토큰 발급 (일반 로그인과 동일한 인프라)
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // 2. Refresh Token DB 저장 및 기기 수 제한 (기존 로직 동일)
        refreshTokenRepository.save(new RefreshToken(email, tokenInfo.getRefreshToken()));
        refreshTokenRepository.deleteOldTokensKeepLatest(email, MAX_DEVICE_COUNT);

        // 3. HttpOnly 쿠키 세팅
        int accessMaxAge  = (int) (jwtTokenProvider.getAccessTokenExpiration()  / 1000);
        int refreshMaxAge = (int) (jwtTokenProvider.getRefreshTokenExpiration() / 1000);
        addCookie(response, AppConstants.ACCESS_TOKEN,  tokenInfo.getAccessToken(),  accessMaxAge);
        addCookie(response, AppConstants.REFRESH_TOKEN, tokenInfo.getRefreshToken(), refreshMaxAge);

        // 4. 프론트엔드로 리다이렉트 (쿼리 파라미터 없이 - 토큰은 쿠키로 전달)
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri).build().toUriString();
        log.debug("OAuth2 로그인 후 리다이렉트 → {}", targetUrl);

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path(AppConstants.COOKIE_PATH_ROOT)
                .httpOnly(true)
                .maxAge(maxAge);

        if (cookieSecure) {
            builder.secure(true).sameSite("None");
        } else {
            builder.secure(false).sameSite("Lax");
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
