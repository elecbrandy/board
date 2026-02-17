package com.elecbrandy.board.global.jwt;

import com.elecbrandy.board.global.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);

        // Content-Type 설정
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> errorResponse = ApiResponse.fail("인증에 실패했습니다. (토큰 만료 또는 유효하지 않음)");

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}