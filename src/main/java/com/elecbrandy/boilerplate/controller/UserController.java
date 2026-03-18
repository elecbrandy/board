package com.elecbrandy.boilerplate.controller;

import com.elecbrandy.boilerplate.domain.dto.RegisterRequest;
import com.elecbrandy.boilerplate.domain.dto.RegisterResponse;
import com.elecbrandy.boilerplate.global.response.CommonResponse;
import com.elecbrandy.boilerplate.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "02. User", description = "회원 관리 API")
@RestController
@RequestMapping("/api/users") // URL 분리
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입")
    @PostMapping("/register")
    public CommonResponse<RegisterResponse> signup(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse userResponse = userService.register(request);
        return CommonResponse.success("회원가입 성공", userResponse);
    }
}
