package com.elecbrandy.boilerplate.global.jwt;

import com.elecbrandy.boilerplate.global.response.ApiResponse;
import com.elecbrandy.boilerplate.global.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {

        // 1. Filter에서 담아둔 에러 코드 꺼내기
        ErrorCode errorCode = (ErrorCode) request.getAttribute("exception");

        // 2. 만약 에러 코드가 없다면 (예: 토큰이 아예 없는 경우) -> "로그인 필요" 처리
        if (errorCode == null) {
            errorCode = ErrorCode.LOGIN_REQUIRED;
        }

        // 3. 응답 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");

        log.warn("Authentication Failed: {} - {}", errorCode.getCode(), errorCode.getMessage());

        ApiResponse<Void> errorResponse = ApiResponse.fail(errorCode);

        // 4. JSON 쓰기
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}