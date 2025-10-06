package ch.mycargogate.fluentValidator;

import java.util.*;
import java.util.function.Predicate;

import static ch.mycargogate.fluentValidator.FieldName.nameOf;

/**
 * Core Validator with fluent API
 */
public class Validator<T> {

    private final List<FieldRule<Object>> fieldRules = new ArrayList<>();
    private final List<CollectionRule<Object>> collectionRules = new ArrayList<>();
    private final List<ObjectRule<T>> objectRules = new ArrayList<>();

    // chained validator for  the super class
    private Validator<?> extendsValidator = null;

    private Validator() {
    }

    public Validator<?> getExtendsValidator() {
        return extendsValidator;
    }

    public <V> void setExtendsValidator(Validator<V> extendsValidator) {

        // TODO: How to check if V subClass T ?
        this.extendsValidator = extendsValidator;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    public ValidationResult validate(T object) {
        return validate(null, object);
    }

    public ValidationResult validate(String holder, T object) {
        return validate(holder, object, null);
    }

    public ValidationResult validate(String holder, T object, String collection, int index) {
        return validate(holder, object, collection + "[" + index + "]");
    }

    public ValidationResult validate(String holder, T object, String prefix) {

        List<ValidationError> errors = new ArrayList<>();

        if( object instanceof  Collection<?> c) {
            c.forEach(e -> {
                T t = (T)e;
                errors.addAll(validate(t).getErrors());
            });
        }

        if( extendsValidator != null) {
            @SuppressWarnings("unchecked")
            var val = (Validator<T>)extendsValidator;
            errors.addAll(val.validate(object).getErrors());
        }

        // field rules
        for (var rule : fieldRules) {
            rule.validate(holder, object, errors);
        }

        // field rules
        for (var rule : collectionRules) {
            rule.validate(holder, object, errors);
        }

        // field rules
        if( object instanceof Collection collection) {
            for (var rule : collectionRules) {
                rule.validate(holder, collection, errors);
            }
        }

        // object rules
        for (var rule : objectRules) {
            if (!rule.getPredicate().test(object)) {
                errors.add(new ValidationError(holder, null, "ObjectRule", rule.getMessage()));
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }

    public static class Builder<T> {
        private final Validator<T> validator = new Validator<>();
        private Object currentFieldRule;
        private Validator<?> extendsValidator = null;

        public Builder<T> extendsValidator(Validator<?> extendsValidator) {
            validator.setExtendsValidator(extendsValidator);
            return this;
        }

        private void validateCurrentField(Object newFieldRole) {
            if (currentFieldRule != null) {
                if (currentFieldRule instanceof FieldRule<?> fr_erased) {

                    @SuppressWarnings("unchecked")
                    var fr = (FieldRule<Object>) fr_erased;
                    validator.fieldRules.add(fr);

                } else if (currentFieldRule instanceof CollectionRule) {

                    @SuppressWarnings("unchecked")
                    var cr = (CollectionRule<Object>)currentFieldRule;
                    validator.collectionRules.add(cr);
                }
            }

            currentFieldRule = newFieldRole;
        }

        public <F> FieldRule.Builder<T, F> fieldRule(Class<F> schema, String fieldName) {
            var rule = new FieldRule<F>(schema, fieldName);
            validateCurrentField(rule);
            return new FieldRule.Builder<T, F>(rule, this);
        }

        public <F> FieldRule.Builder<T, F> fieldRule(FieldReference<T, F> ref) {
            var rule = new FieldRule<F>(null, nameOf(ref));
            validateCurrentField(rule);
            return new FieldRule.Builder<T, F>(rule, this);
        }

        public <E> CollectionRule.Builder<T, E> collectionRule(FieldReference<T, Collection<E>> ref) {
            var rule = new CollectionRule<E>(null, nameOf(ref));
            validateCurrentField(rule);
            return new CollectionRule.Builder<T, E>(rule, this);
        }

        public Builder<T> objectRule(Predicate<T> predicate, String message) {
            var rule = new ObjectRule<T>(predicate, message);
            validateCurrentField(rule);
            validator.objectRules.add(rule);

            return this;
        }

        public Validator<T> build() {
            validateCurrentField(null);
            currentFieldRule = null;
            return validator;
        }
    }
}
