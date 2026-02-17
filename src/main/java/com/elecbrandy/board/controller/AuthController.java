package com.elecbrandy.board.controller;

import com.elecbrandy.board.domain.dto.LoginRequest;
import com.elecbrandy.board.domain.dto.RegisterRequest;
import com.elecbrandy.board.domain.dto.RegisterResponse;
import com.elecbrandy.board.domain.dto.TokenInfo;
import com.elecbrandy.board.global.jwt.JwtTokenProvider;
import com.elecbrandy.board.global.response.ApiResponse;
import com.elecbrandy.board.service.UserService;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider; // 쿠키 만료시간 계산용

    @Value("${jwt.cookie-secure}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> signup(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse userResponse = userService.register(request);
        return ApiResponse.success("회원가입 성공", userResponse);
    }

    @PostMapping("/login")
    public ApiResponse<String> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenInfo tokenInfo = userService.login(request);
        setTokenCookies(response, tokenInfo); // 쿠키 설정 메서드 분리
        return ApiResponse.success("로그인 성공");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. 리프레시 토큰 가져오기 (DB 삭제를 위해 필요)
        String refreshToken = resolveToken(request, "refreshToken");

        // 2. 서비스 로그아웃 호출 (DB 삭제)
        userService.logout(refreshToken);

        // 3. 클라이언트 쿠키 삭제
        setCookie(response, "accessToken", "", 0);
        setCookie(response, "refreshToken", "", 0);

        return ApiResponse.success("로그아웃 성공");
    }

    @PostMapping("/reissue")
    public ApiResponse<Void> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveToken(request, "refreshToken");

        try {
            // 1. 서비스 재발급 호출 (검증, RTR, DB업데이트 모두 수행)
            TokenInfo newToken = userService.reissue(refreshToken);

            // 2. 성공 시 새 쿠키 발급
            setTokenCookies(response, newToken);
            return ApiResponse.success("토큰 재발급 성공");

        } catch (Exception e) {
            // 3. 실패(보안 위협 등) 시 쿠키 삭제 강제 로그아웃 처리
            setCookie(response, "accessToken", "", 0);
            setCookie(response, "refreshToken", "", 0);
            throw e; // GlobalExceptionHandler가 처리하도록 던짐
        }
    }

    // [리팩토링] 토큰 쿠키 설정 공통화
    private void setTokenCookies(HttpServletResponse response, TokenInfo tokenInfo) {
        long accessTokenExp = jwtTokenProvider.getAccessTokenExpiration();
        long refreshTokenExp = jwtTokenProvider.getRefreshTokenExpiration();

        setCookie(response, "accessToken", tokenInfo.getAccessToken(), (int) (accessTokenExp / 1000));
        setCookie(response, "refreshToken", tokenInfo.getRefreshToken(), (int) (refreshTokenExp / 1000));
    }

    private String resolveToken(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    private void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .maxAge(maxAge)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
