package com.dmclarnon.golf.login_api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordsMatchValidator.class)
public @interface PasswordsMatch {
    String message() default "Password do not match!";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
