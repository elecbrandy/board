package com.elecbrandy.boilerplate.auth.domain.dto;

import com.elecbrandy.boilerplate.auth.validator.EmailValid;
import com.elecbrandy.boilerplate.auth.validator.PasswordValid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 요청")
@Getter @Setter @NoArgsConstructor
public class LoginRequest {
    @Schema(description = "사용자 이메일", example = "user@example.com")
    @EmailValid
    private String email;

    @Schema(description = "비밀번호", example = "Password123!")
    @NotBlank
    private String password;

}
