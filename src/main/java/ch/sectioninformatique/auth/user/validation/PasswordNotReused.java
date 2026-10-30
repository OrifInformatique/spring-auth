package ch.sectioninformatique.auth.user.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Class-level constraint for {@link ch.sectioninformatique.auth.user.dto.PasswordUpdateDto}:
 * the new password must differ from the old one. The violation is reported on the
 * {@code newPassword} field.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordNotReusedValidator.class)
public @interface PasswordNotReused {

    String message() default "{validation.password.not.reused}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
