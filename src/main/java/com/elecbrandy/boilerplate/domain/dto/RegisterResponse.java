package com.elecbrandy.boilerplate.domain.dto;

import com.elecbrandy.boilerplate.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "회원가입 응답")
@Getter
public class RegisterResponse {

    @Schema(description = "유저 ID", example = "1")
    private final Long id;

    @Schema(description = "이메일", example = "newuser@example.com")
    private final String email;

    @Schema(description = "닉네임", example = "홍길동")
    private final String username;

    public RegisterResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
    }
}
