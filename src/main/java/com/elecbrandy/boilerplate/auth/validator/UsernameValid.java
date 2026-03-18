package com.elecbrandy.boilerplate.auth.validator;

import com.elecbrandy.boilerplate.auth.constants.AuthConstants;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

@Size(min = AuthConstants.USERNAME_MIN, max = AuthConstants.USERNAME_MAX, message = AuthConstants.USERNAME_MESSAGE)
@Pattern(regexp = AuthConstants.USERNAME_REGEX, message = AuthConstants.USERNAME_MESSAGE)
@NotBlank(message = "닉네임은 필수 입력 값입니다.")
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface UsernameValid {
    String message() default AuthConstants.USERNAME_MESSAGE;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}