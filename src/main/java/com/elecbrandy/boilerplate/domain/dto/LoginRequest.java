package com.elecbrandy.boilerplate.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 요청")
@Getter @Setter @NoArgsConstructor
public class LoginRequest {
    @Schema(description = "사용자 이메일", example = "user@example.com")
    @NotBlank @Email
    private String email;

    @Schema(description = "비밀번호", example = "Password123!")
    @NotBlank
    private String password;

}
