package com.shou.lims.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PhoneValidatorTest {

    private final PhoneValidator validator = new PhoneValidator();

    @Test
    void shouldAcceptValidPhone() {
        assertThat(validator.isValid("13800138000", mock(ConstraintValidatorContext.class))).isTrue();
    }

    @Test
    void shouldRejectShortNumber() {
        assertThat(validator.isValid("1234", mock(ConstraintValidatorContext.class))).isFalse();
    }

    @Test
    void shouldRejectNonDigits() {
        assertThat(validator.isValid("abcdefghijk", mock(ConstraintValidatorContext.class))).isFalse();
    }

    @Test
    void shouldAcceptNull() {
        assertThat(validator.isValid(null, mock(ConstraintValidatorContext.class))).isTrue();
    }

    @Test
    void shouldAcceptEmpty() {
        assertThat(validator.isValid("", mock(ConstraintValidatorContext.class))).isTrue();
    }
}
