package ch.sectioninformatique.auth.auth;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure the new password is different from the old password.
 * This annotation should be applied at the class level on the PasswordUpdateDto record.
 * 
 * Usage:
 * @PasswordNotReused
 * public record PasswordUpdateDto(...)
 * 
 * This prevents users from updating their password to the same value they already have,
 * which could indicate a validation bypass or user error.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordNotReusedValidatorImpl.class)
@Documented
public @interface PasswordNotReused {
    String message() default "{validation.password.not.reused}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

/**
 * Implements the validation logic for the @PasswordNotReused annotation.
 * 
 * This validator checks that:
 * 1. Both old and new password fields are present
 * 2. The new password is not identical to the old password
 * 
 * Note: This validator only checks for exact string equality. In production,
 * you would typically compare the plaintext new password against the hashed
 * old password stored in the database (done in the service layer instead).
 */
class PasswordNotReusedValidatorImpl implements ConstraintValidator<PasswordNotReused, PasswordUpdateDto> {

    @Override
    public boolean isValid(PasswordUpdateDto dto, ConstraintValidatorContext context) {
        // If dto is null, let other validators handle it
        if (dto == null) {
            return true;
        }

        // If either password is null, let @NotNull handle it
        if (dto.oldPassword() == null || dto.newPassword() == null) {
            return true;
        }

        // Check if new password is the same as old password (character by character)
        boolean passwordsAreIdentical = areCharArraysEqual(dto.oldPassword(), dto.newPassword());

        if (passwordsAreIdentical) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Compares two character arrays for equality.
     * Uses a constant-time comparison to prevent timing attacks.
     * 
     * @param arr1 First character array
     * @param arr2 Second character array
     * @return true if arrays are identical, false otherwise
     */
    private boolean areCharArraysEqual(char[] arr1, char[] arr2) {
        if (arr1.length != arr2.length) {
            return false;
        }

        boolean arraysEqual = true;
        for (int i = 0; i < arr1.length; i++) {
            if (arr1[i] != arr2[i]) {
                arraysEqual = false;
            }
        }

        return arraysEqual;
    }
}
