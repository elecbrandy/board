package com.elecbrandy.boilerplate.controller;

import com.elecbrandy.boilerplate.domain.dto.LoginRequest;
import com.elecbrandy.boilerplate.domain.dto.RegisterRequest;
import com.elecbrandy.boilerplate.domain.dto.RegisterResponse;
import com.elecbrandy.boilerplate.domain.dto.TokenInfo;
import com.elecbrandy.boilerplate.global.constants.AppConstants;
import com.elecbrandy.boilerplate.global.jwt.JwtTokenProvider;
import com.elecbrandy.boilerplate.global.response.ApiResponse;
import com.elecbrandy.boilerplate.service.UserService;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Arrays;

@Tag(name = "Auth", description = "인증 API (회원가입, 로그인, 로그아웃, 토큰 재발급)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.cookie-secure}")
    private boolean cookieSecure;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 (형식 불일치 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 또는 닉네임 중복")
    })
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> signup(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse userResponse = userService.register(request);
        return ApiResponse.success("회원가입 성공", userResponse);
    }

    @Operation(summary = "로그인", description = "이메일, 비밀번호로 로그인합니다. 성공 시 Access/Refresh Token이 쿠키로 설정됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "비밀번호 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "계정 없음")
    })
    @PostMapping("/login")
    public ApiResponse<String> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenInfo tokenInfo = userService.login(request);
        setTokenCookies(response, tokenInfo);
        return ApiResponse.ok("로그인 성공");
    }

    @Operation(summary = "로그아웃", description = "Refresh Token 쿠키를 무효화하고 서버 세션을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveToken(request, AppConstants.REFRESH_TOKEN);

        try {
            userService.logout(refreshToken);
        } finally {
            // 예외 여부와 무관하게 항상 쿠키 삭제
            addCookie(response, AppConstants.ACCESS_TOKEN, "", 0);
            addCookie(response, AppConstants.REFRESH_TOKEN, "", 0);
        }

        return ApiResponse.ok("로그아웃 성공");
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token 쿠키를 이용해 Access/Refresh Token을 재발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰 만료 또는 유효하지 않은 토큰")
    })
    @PostMapping("/reissue")
    public ApiResponse<Void> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveToken(request, AppConstants.REFRESH_TOKEN);

        try {
            // 1. 서비스에서 토큰 재발급 시도 (여기서 실패하면 예외 발생 -> catch 이동)
            TokenInfo newToken = userService.reissue(refreshToken);

            // 2. 성공 시 새로운 쿠키 설정
            setTokenCookies(response, newToken);
            return ApiResponse.ok("토큰 재발급 성공");

        } catch (Exception e) {
            // [수정된 부분] 3. 실패 시 기존 쿠키 삭제 (Max-Age 0으로 설정하여 클라이언트에서 제거 유도)
            // addCookie 메서드를 재사용하여 Secure/SameSite 설정도 일관성 있게 적용
            addCookie(response, AppConstants.ACCESS_TOKEN, "", 0);
            addCookie(response, AppConstants.REFRESH_TOKEN, "", 0);

            // 4. 예외를 다시 던져서 GlobalExceptionHandler가 에러 응답(401 등)을 보내도록 함
            throw e;
        }
    }

    private void setTokenCookies(HttpServletResponse response, TokenInfo tokenInfo) {
        // 밀리초(ms) 단위이므로 1000으로 나누어 초(s) 단위로 변환
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

        // 환경 변수(application.yaml)에 따라 Secure 및 SameSite 정책 분기
        if (cookieSecure) {
            // 운영(HTTPS): Cross-Site 요청 허용을 위해 None 설정
            cookieBuilder.secure(true).sameSite("None");
        } else {
            // 로컬(HTTP): 브라우저 호환성을 위해 Lax 설정
            cookieBuilder.secure(false).sameSite("Lax");
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }
}