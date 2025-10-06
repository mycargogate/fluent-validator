package ch.mycargogate.fluentValidator;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

// ==== FieldRule ====
class FieldRule<F> extends ValueRule<F> {
    private Double min, max;
    private Integer minLength, maxLength;
    private String regex;
    private boolean notBlank = false;
    private Set<String> enumValues;
    private LocalDate notBefore, notAfter;
    private Validator<Object> validator = null;

    // Cached values
    private Method getter;

    FieldRule(Method getter) {
        super(getter);
    }

    FieldRule(Class<F> clazz, String fieldName) {
        super(clazz, fieldName);
    }

    @Override
    void doValidate(String holder, F value, Collection<ValidationError> errors) {
        super.doValidate(holder, value, errors);

        // optional null value
        if(value == null) return;

        // numeric
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (min != null && d < min)
                errors.add(new ValidationError(holder, getFieldName(), "min", "must be ≥ " + min));
            if (max != null && d > max)
                errors.add(new ValidationError(holder, getFieldName(), "max", "must be ≤ " + max));
        }

        // string
        if (value instanceof String s) {

            if (notBlank && s.trim().isEmpty())
                errors.add(new ValidationError(holder, getFieldName(), "notBlank", "must not be blank"));

            if (minLength != null && s.length() < minLength)
                errors.add(new ValidationError(holder, getFieldName(), "minLength", "length must be ≥ " + minLength));

            if (maxLength != null && s.length() > maxLength)
                errors.add(new ValidationError(holder, getFieldName(), "maxLength", "length must be ≤ " + maxLength));

            if (regex != null && !s.matches(regex))
                errors.add(new ValidationError(holder, getFieldName(), "regex", "does not match regex " + regex));

            if (enumValues != null && !enumValues.contains(s))
                errors.add(new ValidationError(holder, getFieldName(), "enum", "must be one of " + enumValues));
        }

        // enum type
        if (value instanceof Enum<?> e) {
            if (enumValues != null && !enumValues.contains(e.name()))
                errors.add(new ValidationError(holder, getFieldName(), "enum", "must be one of " + enumValues));
        }

        // date
        if (value instanceof LocalDate d) {
            if (notBefore != null && d.isBefore(notBefore))
                errors.add(new ValidationError(holder, getFieldName(), "notBefore", "must not be before " + notBefore));

            if (notAfter != null && d.isAfter(notAfter))
                errors.add(new ValidationError(holder, getFieldName(), "notAfter", "must not be after " + notAfter));
        }


        // validators
        if(validator != null) {
            var result = validator.validate(holder, value);
            errors.addAll(result.getErrors());
        }
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public boolean isNotBlank() {
        return notBlank;
    }

    public void setNotBlank(boolean notBlank) {
        this.notBlank = notBlank;
    }

    public Set<String> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(Set<String> enumValues) {
        this.enumValues = enumValues;
    }

    public LocalDate getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(LocalDate notBefore) {
        this.notBefore = notBefore;
    }

    public LocalDate getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(LocalDate notAfter) {
        this.notAfter = notAfter;
    }

    public Validator<Object> getValidator() {
        return validator;
    }

    public void setValidator(Validator<Object> validator) {
        this.validator = validator;
    }

    public static class Builder<T, F> {
        private final FieldRule<F> rule;
        private final Validator.Builder<T> parent;

        Builder(FieldRule<F> rule, Validator.Builder<T> parent) {
            this.rule = rule;
            this.parent = parent;
        }

        public Builder<T, F> mandatory() {
            rule.setMandatory(true);
            return this;
        }

        public Builder<T, F> optional() {
            rule.setMandatory(false);
            return this;
        }

        public Builder<T, F> min(Number min) {
            rule.setMin(min.doubleValue());
            return this;
        }

        public Builder<T, F> max(Number max) {
            rule.setMax(max.doubleValue());
            return this;
        }

        public Builder<T, F> minLength(int len) {
            rule.setMinLength(len);
            return this;
        }

        public Builder<T, F> maxLength(int len) {
            rule.setMaxLength(len);
            return this;
        }

        public Builder<T, F> regex(String pattern) {
            rule.setRegex(pattern);
            return this;
        }

        public Builder<T, F> notBlank() {
            rule.setNotBlank(true);
            return this;
        }

        public Builder<T, F> inEnum(String... values) {
            rule.setEnumValues(new HashSet<>(Arrays.asList(values)));
            return this;
        }

        public Builder<T, F> notBefore(LocalDate date) {
            rule.setNotBefore(date);
            return this;
        }

        public Builder<T, F> notAfter(LocalDate date) {
            rule.setNotAfter(date);
            return this;
        }

        public Builder<T, F> custom(Predicate<F> predicateErase, String message) {
            @SuppressWarnings("unchecked")
            var predicate = (Predicate<Object>) predicateErase;
            rule.getCustomRules().add(new ValueRule.CustomRule(predicate, message));
            return this;
        }

        public Builder<T, F> validate(Validator<F> validatorErase) {
            @SuppressWarnings("unchecked")
            var validator = (Validator<Object>) validatorErase;
            rule.setValidator(validator);
            return this;
        }

        public Validator.Builder<T> done() {
            return parent;
        }
    }
}
