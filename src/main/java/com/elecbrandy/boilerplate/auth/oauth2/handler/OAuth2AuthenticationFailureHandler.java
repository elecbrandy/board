package com.elecbrandy.boilerplate.auth.oauth2.handler;

import com.elecbrandy.boilerplate.global.response.CommonResponse;
import com.elecbrandy.boilerplate.global.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Google OAuth2 인증 실패 시 처리를 담당하는 핸들러입니다.
 * <p>
 * 사용자가 Google 동의 화면에서 취소하거나, Google 측 오류가 발생했을 때 호출됩니다.
 * 프론트엔드의 로그인 실패 페이지로 리다이렉트하며, 에러 코드를 쿼리 파라미터로 전달합니다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("OAuth2 인증 실패: {}", exception.getMessage());

        // 프론트엔드에 error 쿼리 파라미터를 붙여 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", ErrorCode.AUTH_INVALID_TOKEN.getCode())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
