package ch.mycargogate.fluentValidator;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

/**
 * Core Validator with fluent API
 */
public class Validator<T> {

    private final List<FieldRule<T>> fieldRules = new ArrayList<>();
    private final List<ObjectRule<T>> objectRules = new ArrayList<>();

    private Validator() {}

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public ValidationResult validate(T object) {
        List<ValidationError> errors = new ArrayList<>();

        // field rules
        for (FieldRule<T> fr : fieldRules) {
            try {
                Field f = object.getClass().getDeclaredField(fr.fieldName);
                f.setAccessible(true);
                Object value = f.get(object);
                fr.validate(value, errors);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                errors.add(new ValidationError(fr.fieldName, "fieldAccess",
                        "Could not access field: " + e.getMessage()));
            }
        }

        // object rules
        for (ObjectRule<T> or : objectRules) {
            if (!or.predicate.test(object)) {
                errors.add(new ValidationError(null, "objectRule", or.message));
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }

    // ==== Builder ====
    public static class Builder<T> {
        private final Validator<T> validator = new Validator<>();
        private FieldRule<T> currentFieldRule;

        public FieldRuleBuilder<T, Object> fieldRule(String fieldName) {
            if (currentFieldRule != null) {
                validator.fieldRules.add(currentFieldRule);
            }
            currentFieldRule = new FieldRule<>(fieldName);
            return new FieldRuleBuilder<T, Object>(currentFieldRule, this);
        }

        public <F> FieldRuleBuilder<T, F> fieldRule(FieldName.FieldReference<T, F> ref) {
            return (FieldRuleBuilder<T, F>)fieldRule(FieldName.nameOf(ref));
        }

        public Builder<T> objectRule(Predicate<T> predicate, String message) {
            validator.objectRules.add(new ObjectRule<>(predicate, message));
            return this;
        }

        public Validator<T> build() {
            if (currentFieldRule != null) {
                validator.fieldRules.add(currentFieldRule);
                currentFieldRule = null;
            }
            return validator;
        }

        // helper for simpler usage
        public Builder<T> objectRule(Predicate<T> predicate) {
            return objectRule(predicate, "Object rule failed");
        }
    }

    // ==== FieldRule Builder ====
    public static class FieldRuleBuilder<T, F> {
        private final FieldRule<T> rule;
        private final Builder<T> parent;

        FieldRuleBuilder(FieldRule<T> rule, Builder<T> parent) {
            this.rule = rule;
            this.parent = parent;
        }

        public FieldRuleBuilder<T, F> mandatory() { rule.mandatory = true; return this; }
        public FieldRuleBuilder<T, F> optional() { rule.mandatory = false; return this; }

        public FieldRuleBuilder<T, F> min(Number min) { rule.min = min.doubleValue(); return this; }
        public FieldRuleBuilder<T, F> max(Number max) { rule.max = max.doubleValue(); return this; }

        public FieldRuleBuilder<T, F> minLength(int len) { rule.minLength = len; return this; }
        public FieldRuleBuilder<T, F> maxLength(int len) { rule.maxLength = len; return this; }
        public FieldRuleBuilder<T, F> regex(String pattern) { rule.regex = pattern; return this; }
        public FieldRuleBuilder<T, F> notBlank() { rule.notBlank = true; return this; }

        public FieldRuleBuilder<T, F> inEnum(String... values) {
            rule.enumValues = new HashSet<>(Arrays.asList(values));
            return this;
        }

        public FieldRuleBuilder<T, F> minSize(int size) { rule.minSize = size; return this; }
        public FieldRuleBuilder<T, F> maxSize(int size) { rule.maxSize = size; return this; }

        public FieldRuleBuilder<T, F> notBefore(LocalDate date) { rule.notBefore = date; return this; }
        public FieldRuleBuilder<T, F> notAfter(LocalDate date) { rule.notAfter = date; return this; }

        public FieldRuleBuilder<T, F> custom(Predicate<F> predicate, String message) {
            rule.customRules.add(new FieldRule.CustomRule((Predicate<Object>)predicate, message));
            return this;
        }

        public FieldRuleBuilder<T, F> validate(Validator<F> validator) {
            rule.validators.add((Validator<Object>) validator);
            return this;
        }

        public Builder<T> done() {
            return parent;
        }
    }

    @FunctionalInterface
    public interface FieldValidator<T> extends java.io.Serializable {
        List<ValidationError> validate(String fieldName, Object value);
    }

    @FunctionalInterface
    public interface FieldReference<T, R> extends java.io.Serializable {
        R apply(T t);
    }

    // ==== FieldRule ====
    private static class FieldRule<T> {
        private final String fieldName;
        private boolean mandatory = false;
        private Double min, max;
        private Integer minLength, maxLength;
        private String regex;
        private boolean notBlank = false;
        private Set<String> enumValues;
        private Integer minSize, maxSize;
        private LocalDate notBefore, notAfter;
        public static record CustomRule(Predicate<Object> predicate, String message) { }
        private final List<CustomRule> customRules = new ArrayList<>();
        private final List<Validator<Object>> validators = new ArrayList<>();

        FieldRule(FieldName.FieldReference<T, ?> ref) {
            this(FieldName.nameOf(ref));
        }

        FieldRule(String fieldName) {
            this.fieldName = fieldName;
        }

        void validate(Object value, List<ValidationError> errors) {
            if (value == null) {
                if (mandatory) {
                    errors.add(new ValidationError(fieldName, "mandatory",
                            "Field '" + fieldName + "' is mandatory"));
                }
                return;
            }

            // numeric
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                if (min != null && d < min)
                    errors.add(new ValidationError(fieldName, "min",
                            "Field '" + fieldName + "' must be ≥ " + min));
                if (max != null && d > max)
                    errors.add(new ValidationError(fieldName, "max",
                            "Field '" + fieldName + "' must be ≤ " + max));
            }

            // string
            if (value instanceof String s) {
                if (notBlank && s.trim().isEmpty())
                    errors.add(new ValidationError(fieldName, "notBlank",
                            "Field '" + fieldName + "' must not be blank"));
                if (minLength != null && s.length() < minLength)
                    errors.add(new ValidationError(fieldName, "minLength",
                            "Field '" + fieldName + "' length must be ≥ " + minLength));
                if (maxLength != null && s.length() > maxLength)
                    errors.add(new ValidationError(fieldName, "maxLength",
                            "Field '" + fieldName + "' length must be ≤ " + maxLength));
                if (regex != null && !s.matches(regex))
                    errors.add(new ValidationError(fieldName, "regex",
                            "Field '" + fieldName + "' does not match regex " + regex));
                if (enumValues != null && !enumValues.contains(s))
                    errors.add(new ValidationError(fieldName, "enum",
                            "Field '" + fieldName + "' must be one of " + enumValues));
            }

            // enum type
            if (value instanceof Enum<?> e) {
                if (enumValues != null && !enumValues.contains(e.name()))
                    errors.add(new ValidationError(fieldName, "enum",
                            "Field '" + fieldName + "' must be one of " + enumValues));
            }

            // collection
            if (value instanceof Collection<?> col) {
                if (minSize != null && col.size() < minSize)
                    errors.add(new ValidationError(fieldName, "minSize",
                            "Field '" + fieldName + "' size must be ≥ " + minSize));
                if (maxSize != null && col.size() > maxSize)
                    errors.add(new ValidationError(fieldName, "maxSize",
                            "Field '" + fieldName + "' size must be ≤ " + maxSize));
            }

            // date
            if (value instanceof LocalDate d) {
                if (notBefore != null && d.isBefore(notBefore))
                    errors.add(new ValidationError(fieldName, "notBefore",
                            "Field '" + fieldName + "' must not be before " + notBefore));
                if (notAfter != null && d.isAfter(notAfter))
                    errors.add(new ValidationError(fieldName, "notAfter",
                            "Field '" + fieldName + "' must not be after " + notAfter));
            }

            // custom rules
            for (var customRule : customRules) {
                if (!customRule.predicate().test(value)) {
                    errors.add(new ValidationError(fieldName, "custom", customRule.message()));
                }
            }

            // validators
            for (var validator : validators) {
                var result = validator.validate(value);
                errors.addAll(result.getErrors());
            }
        }
    }

    // ==== ObjectRule ====
    private static class ObjectRule<T> {
        private final Predicate<T> predicate;
        private final String message;

        ObjectRule(Predicate<T> predicate, String message) {
            this.predicate = predicate;
            this.message = message;
        }
    }

    // ==== ValidationResult ====
    public static class ValidationResult {
        private final boolean valid;
        private final List<ValidationError> errors;

        private ValidationResult(boolean valid, List<ValidationError> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult fail(List<ValidationError> errors) {
            return new ValidationResult(false, errors);
        }

        public boolean isValid() {
            return valid;
        }

        public List<ValidationError> getErrors() {
            return errors;
        }

        @Override
        public String toString() {
            return valid ? "ValidationResult: OK" : "ValidationResult: " + errors;
        }
    }

    // ==== ValidationError ====
    public static class ValidationError {
        private final String field;
        private final String rule;
        private final String message;

        public ValidationError(String field, String rule, String message) {
            this.field = field;
            this.rule = rule;
            this.message = message;
        }

        public String getField() { return field; }
        public String getRule() { return rule; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return "Error{field=" + field + ", rule=" + rule + ", message=" + message + "}";
        }
    }
}
