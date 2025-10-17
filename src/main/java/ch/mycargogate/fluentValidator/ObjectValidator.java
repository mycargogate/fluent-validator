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

    public static class Builder<T, F> {
        private final ObjectValidator<F> objectValidator;
        private final FluentValidator.Builder<T> parent;

        Builder(ObjectValidator<F> objectValidator, FluentValidator.Builder<T> parent) {
            this.objectValidator = objectValidator;
            this.parent = parent;
        }

        public ObjectValidator.Builder<T, F> predicate(Predicate<F> predicate, String code) {
            return predicate(predicate, code, null);
        }

        public ObjectValidator.Builder<T, F> predicate(Predicate<F> predicate, String code, GetErrorMessageArgs<F> getArgs) {
            objectValidator.addPredicate(predicate, code, getArgs);
            return this;
        }

        public ObjectValidator.Builder<T, F> rule(RuleRunner<F> rule) {
            objectValidator.addRule(rule);
            return this;
        }

        public FluentValidator.Builder<T> done() {
            return parent;
        }
    }}
