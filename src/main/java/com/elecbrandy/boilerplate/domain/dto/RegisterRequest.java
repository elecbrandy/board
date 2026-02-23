package com.elecbrandy.boilerplate.domain.dto;

import com.elecbrandy.boilerplate.domain.entity.User;
import com.elecbrandy.boilerplate.domain.enums.Role;
import com.elecbrandy.boilerplate.global.constants.AppConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "회원가입 요청")
@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    @Schema(description = "이메일", example = "newuser@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Email
    private String email;

    @Schema(
            description = "비밀번호 (8~20자, 영문 + 숫자 + 특수문자 포함)",
            example = "Password1!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Pattern(regexp = AppConstants.PASSWORD_REGEX,
            message = "비밀번호는 8~20자이며 영문, 숫자, 특수문자를 포함해야 합니다.")
    private String password;

    @Schema(description = "닉네임 (2~50자)", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = 2, max = 50)
    private String username;

    // DTO -> Entity
    public User toEntity(String encodedPassword) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .username(username)
                .role(Role.USER)
                .build();
    }
}
