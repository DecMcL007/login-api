package com.dmclarnon.golf.login_api.validation;

import com.dmclarnon.golf.login_api.dto.requests.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, RegisterRequest> {
    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request == null) return true; // nothing to validate
        return request.password1() != null &&
                request.password1().equals(request.password2());
    }
}