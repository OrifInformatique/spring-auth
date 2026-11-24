package ch.sectioninformatique.auth.user.validation;

import java.util.Arrays;

import ch.sectioninformatique.auth.user.dto.PasswordUpdateDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates {@link PasswordNotReused}. Null passwords are left to {@code @NotNull}.
 */
public class PasswordNotReusedValidator implements ConstraintValidator<PasswordNotReused, PasswordUpdateDto> {

    @Override
    public boolean isValid(PasswordUpdateDto dto, ConstraintValidatorContext context) {
        if (dto == null || dto.oldPassword() == null || dto.newPassword() == null) {
            return true;
        }
        if (!Arrays.equals(dto.oldPassword(), dto.newPassword())) {
            return true;
        }

        // Attach the violation to the newPassword field so that clients can display it there
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("newPassword")
                .addConstraintViolation();
        return false;
    }
}
