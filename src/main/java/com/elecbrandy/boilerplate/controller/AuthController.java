package com.elecbrandy.boilerplate.controller;

import com.elecbrandy.boilerplate.controller.docs.AuthApi;
import com.elecbrandy.boilerplate.domain.dto.LoginRequest;
import com.elecbrandy.boilerplate.domain.dto.RegisterRequest;
import com.elecbrandy.boilerplate.domain.dto.RegisterResponse;
import com.elecbrandy.boilerplate.domain.dto.TokenInfo;
import com.elecbrandy.boilerplate.global.constants.AppConstants;
import com.elecbrandy.boilerplate.global.jwt.JwtTokenProvider;
import com.elecbrandy.boilerplate.global.response.CommonResponse;
import com.elecbrandy.boilerplate.service.UserService;
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

/**
 * 사용자 인증(Authentication)과 관련된 HTTP 요청을 처리하는 REST API 컨트롤러입니다.
 * <p>
 * 회원가입, 로그인, 로그아웃, 토큰 재발급(Reissue) 기능을 제공하며,
 * 클라이언트와의 보안 통신을 위해 JWT 토큰을 HttpOnly 쿠키 형태로 관리합니다.
 * </p>
 */
@Tag(name = "01. Auth", description = "인증 API (회원가입, 로그인, 로그아웃, 토큰 재발급)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.cookie-secure}")
    private boolean cookieSecure;

    /**
     * 신규 사용자의 회원가입 요청을 처리합니다.
     *
     * @param request 이메일, 비밀번호, 닉네임 정보를 포함한 회원가입 요청 데이터
     * @return 생성된 사용자의 정보를 포함한 응답 객체
     */
    @PostMapping("/register")
    public CommonResponse<RegisterResponse> signup(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse userResponse = userService.register(request);
        return CommonResponse.success("회원가입 성공", userResponse);
    }

    /**
     * 사용자의 로그아웃 요청을 처리합니다.
     * <p>
     * 서버에 저장된 Refresh Token을 삭제하여 세션을 무효화하고,
     * 클라이언트 측의 토큰 쿠키 만료 시간을 0으로 설정하여 삭제를 유도합니다.
     * DB 작업(로그아웃) 중 예외가 발생하더라도 클라이언트의 쿠키는 항상 삭제되도록 보장합니다.
     * </p>
     *
     * @param request  토큰 쿠키를 추출하기 위한 HTTP 요청 객체
     * @param response 쿠키를 삭제하기 위한 HTTP 응답 객체
     * @return 로그아웃 성공 메시지를 담은 응답 객체
     */
    @Operation(summary = "로그아웃", description = "Refresh Token 쿠키를 무효화하고 서버 세션을 삭제합니다.")
    @PostMapping("/logout")
    public CommonResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveToken(request, AppConstants.REFRESH_TOKEN);

        try {
            userService.logout(refreshToken);
        } finally {
            // 예외 여부와 무관하게 항상 쿠키 삭제
            addCookie(response, AppConstants.ACCESS_TOKEN, "", 0);
            addCookie(response, AppConstants.REFRESH_TOKEN, "", 0);
        }

        return CommonResponse.ok("로그아웃 성공");
    }

    /**
     * 사용자의 로그인 요청을 처리합니다.
     * <p>
     * 로그인 성공 시, 생성된 Access Token과 Refresh Token을
     * 클라이언트의 HttpOnly 쿠키에 저장합니다.
     * </p>
     *
     * @param request  로그인을 위한 이메일, 비밀번호 데이터
     * @param response 쿠키를 설정하기 위한 HTTP 응답 객체
     * @return 로그인 성공 메시지를 담은 응답 객체
     */
    @Operation(summary = "로그인", description = "사용자의 아이디와 비밀번호를 이용하여 인증을 진행하고, Access Token과 Refresh Token을 발급합니다.")
    @PostMapping("/login")
    public CommonResponse<String> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenInfo tokenInfo = userService.login(request);
        setTokenCookies(response, tokenInfo);
        return CommonResponse.ok("로그인 성공");
    }

    /**
     * 만료된 Access Token을 갱신하기 위한 토큰 재발급 요청을 처리합니다.
     * <p>
     * - 쿠키에 저장된 Refresh Token을 검증하여 유효한 경우 새로운 토큰 셋을 발급하고 쿠키를 갱신합니다.<br>
     * - 검증에 실패하거나 공격이 의심되는 경우, 보안을 위해 클라이언트의 기존 쿠키를 즉시 삭제합니다.<br>
     * </p>
     *
     * @param request  Refresh Token 쿠키를 추출하기 위한 HTTP 요청 객체
     * @param response 새로운 쿠키를 설정하거나 기존 쿠키를 삭제하기 위한 HTTP 응답 객체
     * @return 재발급 성공 메시지를 담은 응답 객체
     */
    @Operation(summary = "토큰 재발급", description = "Refresh Token 쿠키를 이용해 Access/Refresh Token을 재발급합니다.")
    @PostMapping("/reissue")
    public CommonResponse<Void> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveToken(request, AppConstants.REFRESH_TOKEN);

        try {
            // 1. 서비스에서 토큰 재발급 시도 (여기서 실패하면 예외 발생 -> catch 이동)
            TokenInfo newToken = userService.reissue(refreshToken);

            // 2. 성공 시 새로운 쿠키 설정
            setTokenCookies(response, newToken);
            return CommonResponse.ok("토큰 재발급 성공");

        } catch (Exception e) {
            // 3. 실패 시 기존 쿠키 삭제 (Max-Age 0으로 설정하여 클라이언트에서 제거 유도)
            addCookie(response, AppConstants.ACCESS_TOKEN, "", 0);
            addCookie(response, AppConstants.REFRESH_TOKEN, "", 0);

            // 4. 예외를 다시 던져서 GlobalExceptionHandler가 에러 응답(401 등)을 보내도록 함
            throw e;
        }
    }

    /**
     * Access Token과 Refresh Token을 HTTP 응답 쿠키로 설정합니다.
     * <p>
     * Provider에 설정된 토큰 만료 시간(ms 단위)을 초(s) 단위로 변환하여 쿠키의 Max-Age에 반영합니다.
     * </p>
     *
     * @param response  쿠키를 추가할 HTTP 응답 객체
     * @param tokenInfo 생성된 토큰 정보를 담은 DTO
     */
    private void setTokenCookies(HttpServletResponse response, TokenInfo tokenInfo) {
        // 밀리초(ms) 단위이므로 1000으로 나누어 초(s) 단위로 변환
        int accessTokenMaxAge = (int) (jwtTokenProvider.getAccessTokenExpiration() / 1000);
        int refreshTokenMaxAge = (int) (jwtTokenProvider.getRefreshTokenExpiration() / 1000);

        addCookie(response, AppConstants.ACCESS_TOKEN, tokenInfo.getAccessToken(), accessTokenMaxAge);
        addCookie(response, AppConstants.REFRESH_TOKEN, tokenInfo.getRefreshToken(), refreshTokenMaxAge);
    }

    /**
     * HTTP 요청의 쿠키 목록에서 특정 이름의 쿠키 값을 추출합니다.
     *
     * @param request    HTTP 요청 객체
     * @param cookieName 추출할 쿠키의 이름
     * @return 찾은 쿠키의 값 (존재하지 않을 경우 null)
     */
    private String resolveToken(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    /**
     * HttpOnly 속성이 적용된 안전한 쿠키를 생성하여 HTTP 응답 헤더에 추가합니다.
     * <p>
     * application.yaml의 {@code jwt.cookie-secure} 설정 값에 따라
     * 운영 환경(HTTPS)에서는 {@code Secure=true}, {@code SameSite=None}으로 설정하고,
     * 로컬 환경(HTTP)에서는 {@code Secure=false}, {@code SameSite=Lax}로 설정합니다.
     * </p>
     *
     * @param response 응답을 보낼 HTTP 객체
     * @param name     쿠키의 이름
     * @param value    쿠키의 값 (토큰 문자열)
     * @param maxAge   쿠키의 유효 시간 (초 단위, 0일 경우 쿠키 즉시 만료)
     */
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