package com.elecbrandy.board.controller;

import com.elecbrandy.board.domain.dto.LoginRequest;
import com.elecbrandy.board.domain.dto.TokenInfo;
import com.elecbrandy.board.global.response.ApiResponse;
import com.elecbrandy.board.domain.dto.RegisterRequest;
import com.elecbrandy.board.domain.dto.RegisterResponse;
import com.elecbrandy.board.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
//    private final AuthSrevice authService;

    // Signup: UserService 호출 (DB에 저장)
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> signup(@Valid @RequestBody RegisterRequest request) {
        // Service 호출
        RegisterResponse userResponse = userService.register(request);

        // Response 포장해 return
        return ApiResponse.success("회원가입 성공", userResponse);
    }

    @PostMapping("/login")
    public ApiResponse<TokenInfo> login(@Valid @RequestBody LoginRequest request) {
        TokenInfo tokenInfo = userService.login(request);
        return ApiResponse.success("로그인 성공", tokenInfo);
    }
}
