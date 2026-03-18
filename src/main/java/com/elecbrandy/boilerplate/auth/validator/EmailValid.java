package com.elecbrandy.boilerplate.auth.validator;

import com.elecbrandy.boilerplate.auth.constants.AuthConstants;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.lang.annotation.*;

@Email(regexp = AuthConstants.EMAIL_REGEX, message = AuthConstants.EMAIL_MESSAGE)
@NotBlank(message = "이메일은 필수 입력 값입니다.")
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface EmailValid {
    String message() default AuthConstants.EMAIL_MESSAGE;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}