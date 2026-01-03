package org.wespeak.lesson.validator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private boolean correct;
    private String feedback;

    public static ValidationResult correct(String feedback) {
        return ValidationResult.builder().correct(true).feedback(feedback).build();
    }

    public static ValidationResult incorrect(String feedback) {
        return ValidationResult.builder().correct(false).feedback(feedback).build();
    }
}
