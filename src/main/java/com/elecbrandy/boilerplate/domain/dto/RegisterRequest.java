package com.elecbrandy.boilerplate.domain.dto;

import com.elecbrandy.boilerplate.auth.validator.EmailValid;
import com.elecbrandy.boilerplate.auth.validator.PasswordValid;
import com.elecbrandy.boilerplate.auth.validator.UsernameValid;
import com.elecbrandy.boilerplate.domain.entity.User;
import com.elecbrandy.boilerplate.domain.enums.Role;
import com.elecbrandy.boilerplate.auth.oauth2.domain.OAuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "회원가입 요청")
@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    @Schema(description = "이메일", example = "newuser@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @EmailValid
    private String email;

    @Schema(
            description = "비밀번호 (8~20자, 영문 + 숫자 + 특수문자 포함)",
            example = "Password1!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @PasswordValid
    private String password;

    @Schema(description = "닉네임 (2~50자)", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    @UsernameValid
    private String username;

    // DTO -> Entity
    public User toEntity(String encodedPassword) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .username(username)
                .role(Role.USER)
                .provider(OAuthProvider.LOCAL)
                .build();
    }
}
