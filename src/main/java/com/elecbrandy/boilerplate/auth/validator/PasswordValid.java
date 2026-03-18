package com.elecbrandy.boilerplate.auth.validator;

import com.elecbrandy.boilerplate.auth.constants.AuthConstants;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.*;

@NotBlank(message = "비밀번호는 필수 입력 값입니다.")
@Pattern(regexp = AuthConstants.PASSWORD_REGEX, message = AuthConstants.PASSWORD_MESSAGE)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {}) // 별도의 Validator 클래스 없이 기본 어노테이션 조합으로 사용
@Documented
public @interface PasswordValid {
    String message() default AuthConstants.PASSWORD_MESSAGE;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}