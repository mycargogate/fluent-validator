package ch.mycargogate.fluentValidator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

class ObjectValidator<F> extends BaseValidator<F> {

    public ObjectValidator(String fieldName) {
        super(fieldName);
    }

    public ObjectValidator() {
        super(null);
    }

    void validate(String holder, F object, List<ValidationError> errors) {
        try {
            doValidate(holder, object, errors);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Builder<T> {
        private final ObjectValidator<T> objectValidator;
        private final FluentValidator.Builder<T> parent;

        Builder(ObjectValidator<T> objectValidator, FluentValidator.Builder<T> parent) {
            this.objectValidator = objectValidator;
            this.parent = parent;
        }

        public ObjectValidator.Builder<T> predicate(Predicate<T> predicate, String code) {
            return predicate(predicate, code, null);
        }

        public ObjectValidator.Builder<T> predicate(Predicate<T> predicate, String code, GetErrorMessageArgs<T> getArgs) {
            objectValidator.addPredicate(predicate, code, getArgs);
            return this;
        }

        public ObjectValidator.Builder<T> rule(RuleRunner<T> rule) {
            objectValidator.addRule(rule);
            return this;
        }

        public FluentValidator.Builder<T> done() {
            return parent;
        }
    }}
