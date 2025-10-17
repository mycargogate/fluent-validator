package ch.mycargogate.fluentValidator;

import lombok.Getter;

import java.util.List;

// ==== ValidationResult ====
@Getter
public class ValidationResult {
    private final boolean valid;
    private final List<ValidationError> errors;

    public ValidationResult(boolean valid, List<ValidationError> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult fail(List<ValidationError> errors) {
        return new ValidationResult(false, errors);
    }

    @Override
    public String toString() {
        return valid ? "ValidationResult: OK" : "ValidationResult: " + errors;
    }
}
