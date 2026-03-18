package com.elecbrandy.boilerplate.auth.controller.docs;

import com.elecbrandy.boilerplate.auth.domain.dto.LoginRequest;
import com.elecbrandy.boilerplate.domain.dto.RegisterRequest;
import com.elecbrandy.boilerplate.domain.dto.RegisterResponse;
import com.elecbrandy.boilerplate.global.annotation.ApiCommonErrorResponse;
import com.elecbrandy.boilerplate.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "01. Auth", description = "인증 API (회원가입, 로그인, 로그아웃, 토큰 재발급)")
@ApiCommonErrorResponse // 공통 400, 401, 500 에러 자동 적용
public interface AuthApi {

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임을 입력받아 신규 사용자를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "회원가입 성공")
    @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일 또는 닉네임",
            content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    CommonResponse<RegisterResponse> signup(RegisterRequest request);

    @Operation(summary = "로그인", description = "이메일, 비밀번호로 로그인합니다. 성공 시 Access/Refresh Token이 HttpOnly 쿠키로 발급됩니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    CommonResponse<String> login(LoginRequest request, HttpServletResponse response);

    @Operation(summary = "로그아웃", description = "Refresh Token 쿠키를 무효화하고 서버 세션을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    CommonResponse<Void> logout(HttpServletRequest request, HttpServletResponse response);

    @Operation(summary = "토큰 재발급", description = "Refresh Token 쿠키를 이용해 Access/Refresh Token을 재발급합니다.")
    @ApiResponse(responseCode = "200", description = "토큰 재발급 성공")
    CommonResponse<Void> reissue(HttpServletRequest request, HttpServletResponse response);
}
