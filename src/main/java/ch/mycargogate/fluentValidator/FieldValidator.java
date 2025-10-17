package ch.mycargogate.fluentValidator;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;

// ==== FieldRule ====

@Getter
@Setter
@Slf4j
class FieldValidator<F> extends ValueValidator<F> {
    public static String EMAIL_REGEX = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    public static String EMAIL_CODE = "INVALID_EMAIL";

    private Double min, max;
    private Integer minLength, maxLength;
    private String regex;
    private boolean notBlank = false;
    private Set<String> enumValues;
    private ZonedDateTime notBefore, notAfter;
    private FluentValidator<F> validator = null;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy - HH:mm:ss Z");

    // Cached values
    private Method getter;

    // Error code can be customized for specific regex.
    private String customCode;

    FieldValidator(Method getter) {
        super(getter);
    }

    FieldValidator(Class<F> clazz, String fieldName) {
        super(clazz, fieldName);
    }

    void validate(String holder, F object, List<ValidationError> errors) {
        try {
            @SuppressWarnings("unchecked")
            F value = (F) getGetter((Class<F>) object.getClass()).invoke(object);

            doValidate(holder, value, errors);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doValidate(String holder, F value, List<ValidationError> errors) {
        super.doValidate(holder, value, errors);

        // optional null value
        if(value == null) return;

        // numeric
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (min != null && d < min) {
                String message = ValidatorMessages.message(ErrorCode.LOWER_THAN_MIN, getFullFieldName(holder), d, min);
                addErrorMessage(holder, errors, ErrorCode.LOWER_THAN_MIN, message);
            }

            if (max != null && d > max) {
                String message = ValidatorMessages.message(ErrorCode.GREATER_THAN_MAX, getFullFieldName(holder), d, max);
                addErrorMessage(holder, errors, ErrorCode.GREATER_THAN_MAX, message);
            }
        }

        // string
        if (value instanceof String s) {

            if (notBlank && s.trim().isEmpty()) {
                String message = ValidatorMessages.message(ErrorCode.NOT_BLANK, getFullFieldName(holder), s);
                addErrorMessage(holder, errors, ErrorCode.NOT_BLANK, message);
            }

            if (minLength != null && s.length() < minLength) {
                String message = ValidatorMessages.message(ErrorCode.LENGTH_LOWER_THAN, getFullFieldName(holder), s.length(), minLength);
                addErrorMessage(holder,  errors, ErrorCode.LENGTH_LOWER_THAN, message);
            }

            if (maxLength != null && s.length() > maxLength) {
                String message = ValidatorMessages.message(ErrorCode.LENGTH_GREATER_THAN, getFullFieldName(holder), s.length(), maxLength);
                addErrorMessage(holder, errors, ErrorCode.LENGTH_GREATER_THAN, message);
            }

            if (regex != null && !s.matches(regex)) {
                String code = customCode == null? ErrorCode.REGEX_DONT_MATCH: customCode;
                String message = ValidatorMessages.message(code, getFullFieldName(holder), s, regex);
                addErrorMessage(holder, errors, ErrorCode.REGEX_DONT_MATCH, message);
            }

            if (enumValues != null && !enumValues.contains(s)) {
                String message = ValidatorMessages.message(ErrorCode.MUST_BE_ONE_OF, getFullFieldName(holder), s, enumValues);
                addErrorMessage(holder, errors, ErrorCode.MUST_BE_ONE_OF, message);
            }
        }

        // enum type
        if (value instanceof Enum<?> e) {

            if (enumValues == null || enumValues.isEmpty()) {
                enumValues = new HashSet<String>(Arrays.stream(e.getDeclaringClass().getEnumConstants()).map(Enum::toString).toList());
                log.info("possible enum values: " + enumValues.toString());
            }

            if (!enumValues.contains(e.name())) {
                String message = ValidatorMessages.message(ErrorCode.MUST_BE_ONE_OF, getFullFieldName(holder), e, enumValues);
                addErrorMessage(holder,errors, ErrorCode.MUST_BE_ONE_OF, message);
            }
        }

        // Convert local date and localdatetime to zoneddatetime
        if (value instanceof LocalDateTime d) {
            var utc = ZoneId.of("UTC");
            //noinspection unchecked
            value = (F)ZonedDateTime.of(d, utc);
        } else if (value instanceof LocalDate d) {
            var utc = ZoneId.of("UTC");
            //noinspection unchecked
            value = (F)ZonedDateTime.of(d, LocalTime.of(0, 0, 0), utc);
        }

        if (value instanceof ZonedDateTime d) {
            if (notBefore != null && d.isBefore(notBefore)) {
                String dateString = d.format(dateFormatter);
                String notBeforeString = notBefore.format(dateFormatter);
                String message = ValidatorMessages.message(ErrorCode.DATE_BEFORE, getFullFieldName(holder), notBeforeString, notBefore);
                addErrorMessage(holder, errors, ErrorCode.DATE_BEFORE, message);
            }

            if (notAfter != null && d.isAfter(notAfter)) {
                String dateString = d.format(dateFormatter);
                String notAfterString = notAfter.format(dateFormatter);
                String message = ValidatorMessages.message(ErrorCode.DATE_AFTER, getFullFieldName(holder), notAfterString, notBefore);
                addErrorMessage(holder, errors, ErrorCode.DATE_AFTER, message);
            }
        }

        // validators
        if(validator != null) {

            String elementHolder = holder == null? value.getClass().getSimpleName(): holder;
            if( value instanceof HolderNode node)
                elementHolder += "[" + node.holderNodeName() + "]";
            else
                elementHolder +=  "." + getFieldName();

            var newHolder = getFullFieldName(holder);
            var result = validator.validate(newHolder, value);
            errors.addAll(result.getErrors());
        }
    }

    private void setNotBefore(LocalDate date) {
        var utc = ZoneId.of("UTC");
        notBefore = ZonedDateTime.of(date, LocalTime.of(0, 0, 0), utc);
    }

    private void setNotAfter(LocalDate date) {
        var utc = ZoneId.of("UTC");
        notAfter = ZonedDateTime.of(date, LocalTime.of(0, 0, 0), utc);
    }

    public static class Builder<T, F> {
        private final FieldValidator<F> fieldValidator;
        private final FluentValidator.Builder<T> parent;

        Builder(FieldValidator<F> fieldValidator, FluentValidator.Builder<T> parent) {
            this.fieldValidator = fieldValidator;
            this.parent = parent;
        }

        public Builder<T, F> mandatory() {
            fieldValidator.setMandatory(true);
            return this;
        }

        public Builder<T, F> optional() {
            fieldValidator.setMandatory(false);
            return this;
        }

        public Builder<T, F> min(Number min) {
            fieldValidator.setMin(min.doubleValue());
            return this;
        }

        public Builder<T, F> max(Number max) {
            fieldValidator.setMax(max.doubleValue());
            return this;
        }

        public Builder<T, F> minLength(int len) {
            fieldValidator.setMinLength(len);
            return this;
        }

        public Builder<T, F> maxLength(int len) {
            fieldValidator.setMaxLength(len);
            return this;
        }

        public Builder<T, F> email() {
            fieldValidator.setRegex(EMAIL_REGEX);
            fieldValidator.setCustomCode(EMAIL_CODE);
            return this;
        }

        public Builder<T, F> regex(String pattern) {
            fieldValidator.setRegex(pattern);
            return this;
        }

        public Builder<T, F> notBlank() {
            fieldValidator.setNotBlank(true);
            return this;
        }

        public Builder<T, F> inEnum(String... values) {
            fieldValidator.setEnumValues(new HashSet<>(Arrays.asList(values)));
            return this;
        }

        public Builder<T, F> notBefore(LocalDate date) {
            fieldValidator.setNotBefore(date);
            return this;
        }

        public Builder<T, F> notAfter(LocalDate date) {
            fieldValidator.setNotAfter(date);
            return this;
        }

        public Builder<T, F> predicate(Predicate<F> predicate, String code) {
            return predicate(predicate, code, null);
        }

        public Builder<T, F> predicate(Predicate<F> predicate, String code, GetErrorMessageArgs<F> getArgs) {
            fieldValidator.addPredicate(predicate, code, getArgs);
            return this;
        }

        public Builder<T, F> rule(RuleRunner<F> rule) {
            fieldValidator.addRule(rule);
            return this;
        }

        public Builder<T, F> validate(FluentValidator<F> fluentValidator) {
            fieldValidator.setValidator(fluentValidator);
            return this;
        }

        public FluentValidator.Builder<T> done() {
            return parent;
        }
    }
}
