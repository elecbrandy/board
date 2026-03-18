package com.elecbrandy.boilerplate.auth.controller;

import com.elecbrandy.boilerplate.auth.domain.dto.LoginRequest;
import com.elecbrandy.boilerplate.auth.domain.dto.TokenInfo;
import com.elecbrandy.boilerplate.auth.jwt.JwtTokenProvider;
import com.elecbrandy.boilerplate.auth.service.AuthService;
import com.elecbrandy.boilerplate.global.constants.AppConstants;
import com.elecbrandy.boilerplate.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@Tag(name = "01. Auth", description = "인증 API (로그인, 로그아웃, 토큰 재발급)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController { // AuthApi 인터페이스는 필요 시 맞게 수정

    private final AuthService authService; // UserService -> AuthService 로 변경
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.cookie-secure}")
    private boolean cookieSecure;

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public CommonResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveToken(request, AppConstants.REFRESH_TOKEN);
        try {
            authService.logout(refreshToken);
        } finally {
            addCookie(response, AppConstants.ACCESS_TOKEN, "", 0);
            addCookie(response, AppConstants.REFRESH_TOKEN, "", 0);
        }
        return CommonResponse.ok("로그아웃 성공");
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public CommonResponse<String> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenInfo tokenInfo = authService.login(request);
        setTokenCookies(response, tokenInfo);
        return CommonResponse.ok("로그인 성공");
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/reissue")
    public CommonResponse<Void> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveToken(request, AppConstants.REFRESH_TOKEN);
        try {
            TokenInfo newToken = authService.reissue(refreshToken);
            setTokenCookies(response, newToken);
            return CommonResponse.ok("토큰 재발급 성공");
        } catch (Exception e) {
            addCookie(response, AppConstants.ACCESS_TOKEN, "", 0);
            addCookie(response, AppConstants.REFRESH_TOKEN, "", 0);
            throw e;
        }
    }

    private void setTokenCookies(HttpServletResponse response, TokenInfo tokenInfo) {
        int accessTokenMaxAge = (int) (jwtTokenProvider.getAccessTokenExpiration() / 1000);
        int refreshTokenMaxAge = (int) (jwtTokenProvider.getRefreshTokenExpiration() / 1000);
        addCookie(response, AppConstants.ACCESS_TOKEN, tokenInfo.getAccessToken(), accessTokenMaxAge);
        addCookie(response, AppConstants.REFRESH_TOKEN, tokenInfo.getRefreshToken(), refreshTokenMaxAge);
    }

    private String resolveToken(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(name, value)
                .path(AppConstants.COOKIE_PATH_ROOT)
                .httpOnly(true)
                .maxAge(maxAge);

        if (cookieSecure) {
            cookieBuilder.secure(true).sameSite("None");
        } else {
            cookieBuilder.secure(false).sameSite("Lax");
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }
}