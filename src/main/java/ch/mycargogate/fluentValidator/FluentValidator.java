package ch.mycargogate.fluentValidator;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Predicate;

import static ch.mycargogate.fluentValidator.FieldName.nameOf;

/**
 * Core Validator with fluent API
 */
@Slf4j
public class FluentValidator<T> {

    private final List<FieldValidator<Object>> fieldValidators = new ArrayList<>();
    private final List<CollectionValidator<Object>> collectionRules = new ArrayList<>();
    private final List<ObjectValidator<T>> objectValidators = new ArrayList<>();

    // chained validator for  the super class
    private FluentValidator<?> extendsFluentValidator = null;

    public FluentValidator() {
    }

    public FluentValidator<?> getExtendsValidator() {
        return extendsFluentValidator;
    }

    public <V> void setExtendsValidator(FluentValidator<V> extendsFluentValidator) {

        // TODO: How to check if V subClass T ?
        this.extendsFluentValidator = extendsFluentValidator;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public ValidationResult validate(T object) {
        String holder;

        if( object instanceof HolderNode node)
            holder = object.getClass().getSimpleName() + "[" + node.holderNodeName() + "]";
        else
            holder = object.getClass().getSimpleName();

        return validate(holder, object);
    }

    public ValidationResult validate(String holder, T object) {
        log.debug("validate holder=" + holder + ", object class=" + object.getClass().getSimpleName());

        List<ValidationError> errors = new ArrayList<>();

        if(holder == null) holder = getClass().getSimpleName();

        if( extendsFluentValidator != null) {

            @SuppressWarnings("unchecked")
            var val = (FluentValidator<T>) extendsFluentValidator;
            errors.addAll(val.validate(holder, object).getErrors());
        }

        for (var rule : fieldValidators) {
            rule.validate(holder, object, errors);
        }

        for (var rule : collectionRules)
            rule.validate(holder, object, errors);

        // object rules
        for (var rule : objectValidators)
            rule.validate(holder, object, errors);

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }

    public static class Builder<T> {
        private final FluentValidator<T> fluentValidator = new FluentValidator<>();
        private Object currentValidator;
        private FluentValidator<?> extendsFluentValidator = null;

        public Builder<T> extendsValidator(FluentValidator<?> extendsFluentValidator) {
            fluentValidator.setExtendsValidator(extendsFluentValidator);
            return this;
        }

        private void validateCurrentField(Object newFieldRole) {
            if (currentValidator != null) {
                if (currentValidator instanceof FieldValidator<?>) {

                    @SuppressWarnings("unchecked")
                    var fieldValidator = (FieldValidator<Object>) currentValidator;
                    fluentValidator.fieldValidators.add(fieldValidator);

                } else if (currentValidator instanceof CollectionValidator) {

                    @SuppressWarnings("unchecked")
                    var collectionValidator = (CollectionValidator<Object>) currentValidator;
                    fluentValidator.collectionRules.add(collectionValidator);

                } else if (currentValidator instanceof ObjectValidator<?>) {

                    @SuppressWarnings("unchecked")
                    var objectValidator = (ObjectValidator<T>)currentValidator;

                    fluentValidator.objectValidators.add(objectValidator);
                } else
                    throw new RuntimeException("Fix the code, unknown rule type: " + currentValidator.getClass());
            }

            currentValidator = newFieldRole;
        }

        public <F> FieldValidator.Builder<T, F> fieldRule(Class<F> schema, String fieldName) {
            var rule = new FieldValidator<F>(schema, fieldName);
            validateCurrentField(rule);
            return new FieldValidator.Builder<T, F>(rule, this);
        }

        public <F> FieldValidator.Builder<T, F> fieldRule(GetterRef<T, F> ref) {
            var rule = new FieldValidator<F>(null, nameOf(ref));
            validateCurrentField(rule);
            return new FieldValidator.Builder<T, F>(rule, this);
        }

        public <E> CollectionValidator.Builder<T, E> collectionRule(GetterRef<T, Collection<E>> ref) {
            var rule = new CollectionValidator<E>(null, nameOf(ref));
            validateCurrentField(rule);
            return new CollectionValidator.Builder<T, E>(rule, this);
        }

        public Builder<T> objectRule(Predicate<T> predicate, String code) {
            return objectRule(predicate, code, null);
        }

        public Builder<T> objectRule(Predicate<T> predicate, String code, GetErrorMessageArgs<T> getArgs) {
            var objectValidator = new ObjectValidator<T>();

            objectValidator.addPredicate(predicate, code, getArgs);

            validateCurrentField(objectValidator);

            return this;
        }

        public FluentValidator<T> build() {
            validateCurrentField(null);
            currentValidator = null;
            return fluentValidator;
        }
    }
}
