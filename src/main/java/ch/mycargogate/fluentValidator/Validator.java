package ch.mycargogate.fluentValidator;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

import static ch.mycargogate.fluentValidator.FieldName.nameOf;

/**
 * Core Validator with fluent API
 */
public class Validator<T> {

    private final List<FieldRule<T>> fieldRules = new ArrayList<>();
    private final List<CollectionRule<T, ?>> collectionRules = new ArrayList<>();
    private final List<ObjectRule<T>> objectRules = new ArrayList<>();

    // chained validator for  the super class
    private Validator<? extends Object> extendsValidator = null;

    private Validator() {
    }

    public Validator<? extends Object> getExtendsValidator() {
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
        if(prefix == null)
            prefix = "";
        else
            prefix = prefix + ".";

        List<ValidationError> errors = new ArrayList<>();

        if( object instanceof  Collection<?> c) {
            c.forEach(e -> {
                T t = (T)e;
                errors.addAll(validate(t).getErrors());
            });
        }

        if( extendsValidator != null) {
            Validator<T> val = (Validator<T>)extendsValidator;
            errors.addAll(val.validate(object).getErrors());
        }

        // field rules
        for (FieldRule<T> fr : fieldRules) {
            String readableFieldName = prefix + fr.fieldName;
            try {
                Field f = null;
                Class<?> clazz = object.getClass();
                do {
                    try {
                        f = clazz.getDeclaredField(fr.fieldName);
                        break;
                    } catch(Exception e) {}
                } while((clazz = clazz.getSuperclass()) != Object.class);

                if(f == null)
                    throw new RuntimeException(String.format("Field %s is not accesible in the class %s or a super class", fr.fieldName, object.getClass()));

                f.setAccessible(true);

                var elementHolder = holder == null? null: holder + fr.fieldName;
                Object value = f.get(object);
                fr.validate(elementHolder, value, errors);

            } catch (IllegalAccessException e) {
                errors.add(new ValidationError(holder, fr.fieldName, "fieldAccess",
                        "Could not access field: " + e.getMessage()));
            }
        }

        // field rules
        int i = -1;
        for (CollectionRule<T, ?> fr_erased : collectionRules) {
            i++;

            @SuppressWarnings("unchecked")
            var fr = (CollectionRule<T, Object>)fr_erased;

            try {
                Field f = null;
                Class<?> clazz = object.getClass();
                do {
                    try {
                        f = clazz.getDeclaredField(fr.fieldName);
                        break;
                    } catch(Exception e) {}
                } while((clazz = clazz.getSuperclass()) != Object.class);

                if(f == null)
                    throw new RuntimeException(String.format("Field %s is not accesible in the class %s or a super class", fr.fieldName, object.getClass()));

                f.setAccessible(true);

                @SuppressWarnings("unchecked")
                var value = (Collection<Object>)f.get(object);

                fr.validate(holder, value, errors);
            } catch (IllegalAccessException e) {
                errors.add(new ValidationError(holder, fr.fieldName, "fieldAccess",
                        "Could not access field: " + e.getMessage()));
            }
        }

        // object rules
        for (ObjectRule<T> or : objectRules) {
            if (!or.predicate.test(object)) {
                errors.add(new ValidationError(holder, null, "ObjectRule", or.message));
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }

    // ==== Builder ====
    public static class Builder<T> {
        private final Validator<T> validator = new Validator<>();
        private Object currentFieldRule;
        private Validator<? extends Object> extendsValidator = null;

        public  Builder<T> extendsValidator(Validator<? extends Object> extendsValidator) {
            validator.setExtendsValidator(extendsValidator);
            return this;
        }

        private void validateCurrentField(Object newFieldRole) {
            if (currentFieldRule != null) {
                if(currentFieldRule instanceof Validator.FieldRule<?> fr_erased) {

                    @SuppressWarnings("unchecked")
                    var fr = (Validator.FieldRule<T>) fr_erased;
                    validator.fieldRules.add(fr);

                } else if( currentFieldRule instanceof CollectionRule<?, ?> cr_erased) {
                    @SuppressWarnings("unchecked")
                    var cr = (Validator.CollectionRule<T, ?>) cr_erased;
                    validator.collectionRules.add(cr);
                }
            }

            currentFieldRule = newFieldRole;
        }
        public <F> FieldRuleBuilder<T, ?> fieldRule(Class<T> schema, String fieldName) {
            try {
                var field = schema.getDeclaredField(fieldName);
                Class<?> fieldType = field.getType();
                String capitalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                String getterName;
                if (fieldType == boolean.class || fieldType == Boolean.class) {
                    getterName = "is" + capitalized;
                } else {
                    getterName = "get" + capitalized;
                }

                var getter = schema.getMethod(getterName);

                return fieldRule(c -> {
                    try {
                        return getter.invoke(c);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

            } catch (NoSuchFieldException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public <F> FieldRuleBuilder<T, F> fieldRule(FieldName.FieldReference<T, F> ref) {

            @SuppressWarnings("unchecked")
            var fr = new FieldRule<T>(ref);
            validateCurrentField(fr);
            return new FieldRuleBuilder<T, F>(fr, this);
        }

        public <E> CollectionRuleBuilder<T, E> collectionRule(FieldName.FieldReference<T, Collection<E>> ref) {
            var rule = new CollectionRule<T, E>(ref);
            validateCurrentField(rule);
            return new CollectionRuleBuilder<T, E>(rule, this);
        }

        public Builder<T> objectRule(Predicate<T> predicate, String message) {
            var or = new ObjectRule<>(predicate, message);
            validateCurrentField(or);
            validator.objectRules.add(or);

            return this;
        }

        public Validator<T> build() {
            validateCurrentField(null);
            currentFieldRule = null;
            return validator;
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
    public interface ObjectValidator<T> extends java.io.Serializable {
        List<ValidationError> validate(Object value);
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
            this(nameOf(ref));
        }

        FieldRule(String fieldName) {
            this.fieldName = fieldName;
        }

        void validate(Collection<Object> values, List<ValidationError> errors) {
            values.forEach(v -> validate(null, v, errors));
        }

        void validate(String elementHolder, Object value, List<ValidationError> errors) {

            if(elementHolder == null)
                elementHolder = "";
            else
                elementHolder = elementHolder + ".";

            if (value == null) {
                if (mandatory) {
                    errors.add(new ValidationError(elementHolder, fieldName, "mandatory", "is mandatory"));
                }
                return;
            }

            // numeric
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                if (min != null && d < min)
                    errors.add(new ValidationError(elementHolder, fieldName, "min","must be ≥ " + min));
                if (max != null && d > max)
                    errors.add(new ValidationError(elementHolder, fieldName, "max", "must be ≤ " + max));
            }

            // string
            if (value instanceof String s) {
                if (notBlank && s.trim().isEmpty())
                    errors.add(new ValidationError(elementHolder, fieldName, "notBlank","must not be blank"));
                if (minLength != null && s.length() < minLength)
                    errors.add(new ValidationError(elementHolder, fieldName, "minLength", "length must be ≥ " + minLength));
                if (maxLength != null && s.length() > maxLength)
                    errors.add(new ValidationError(elementHolder, fieldName, "maxLength","length must be ≤ " + maxLength));
                if (regex != null && !s.matches(regex))
                    errors.add(new ValidationError(elementHolder, fieldName, "regex","does not match regex " + regex));
                if (enumValues != null && !enumValues.contains(s))
                    errors.add(new ValidationError(elementHolder, fieldName, "enum", "must be one of " + enumValues));
            }

            // enum type
            if (value instanceof Enum<?> e) {
                if (enumValues != null && !enumValues.contains(e.name()))
                    errors.add(new ValidationError(elementHolder, fieldName, "enum","must be one of " + enumValues));
            }

            // collection
            if (value instanceof Collection<?> col) {
                if (minSize != null && col.size() < minSize)
                    errors.add(new ValidationError(elementHolder, fieldName, "minSize", "size must be ≥ " + minSize));
                if (maxSize != null && col.size() > maxSize)
                    errors.add(new ValidationError(elementHolder, fieldName, "maxSize", "size must be ≤ " + maxSize));

                // validate collection of objects
                col.forEach(c -> this.validators.forEach( v -> v.validate(c)));
            }

            // date
            if (value instanceof LocalDate d) {
                if (notBefore != null && d.isBefore(notBefore))
                    errors.add(new ValidationError(elementHolder, fieldName, "notBefore", "must not be before " + notBefore));
                if (notAfter != null && d.isAfter(notAfter))
                    errors.add(new ValidationError(elementHolder, fieldName, "notAfter","must not be after " + notAfter));
            }

            // custom rules
            for (var customRule : customRules) {
                if (!customRule.predicate().test(value)) {
                    errors.add(new ValidationError(elementHolder, fieldName, "custom", customRule.message()));
                }
            }

            // validators
            for (var validator : validators) {
                var result = validator.validate(elementHolder, value);
                errors.addAll(result.getErrors());
            }
        }
    }

    // ==== ListRuleBuilder ======
    public static class CollectionRuleBuilder<T, E> {
        private final CollectionRule<T, E> rule;
        private final Builder<T> parent;

        CollectionRuleBuilder(CollectionRule<T, E> rule, Builder<T> parent) {
            this.rule = rule;
            this.parent = parent;
        }

        public CollectionRuleBuilder<T, E> mandatory() { rule.mandatory = true; return this; }
        public CollectionRuleBuilder<T, E> optional() { rule.mandatory = false; return this; }

        public CollectionRuleBuilder<T, E> minSize(int size) { rule.minSize = size; return this; }
        public CollectionRuleBuilder<T, E> maxSize(int size) { rule.maxSize = size; return this; }

        public CollectionRuleBuilder<T, E> elementValidator(Validator<E> elementValidator) {
            rule.elementValidator = elementValidator;
            return this;
        }

        public Builder<T> done() {
            return parent;
        }
    }

    // ==== ListRule ====
    private static class CollectionRule<T, E> {
        private final String fieldName;
        private boolean mandatory = false;
        private Integer minSize, maxSize;
        private Validator<E> elementValidator;

        CollectionRule(FieldName.FieldReference<T, Collection<E>> ref) {
            this.fieldName = nameOf(ref);
        }

        void validate(String holder, Collection<E> collection, Collection<ValidationError> errors) {
            if (collection == null) {
                if (mandatory) {
                    errors.add(new ValidationError(holder, fieldName, "mandatory", "is mandatory"));
                }
                return;
            }

            if (minSize != null && collection.size() < minSize)
                errors.add(new ValidationError(holder, fieldName, "minSize", "size must be ≥ " + minSize));
            if (maxSize != null && collection.size() > maxSize)
                errors.add(new ValidationError(holder, fieldName, "maxSize", "size must be ≤ " + maxSize));

            if(elementValidator != null) {
                int index = 0;
                for (E e : collection) {
                    var result = elementValidator.validate(holder, e, fieldName, index);
                    errors.addAll(result.getErrors());
                    index++;
                }
            }
        }
    }

    // ==== ObjectRule ====
    private static class ObjectRule<T> {
        private final Predicate<T> predicate;
        private final String message;
        private final List<ObjectValidator<T>> lambdas = new ArrayList<>();

        ObjectRule(Predicate<T> predicate, String message) {
            this.predicate = predicate;
            this.message = message;
        }
    }

    // ==== ValidationResult ====
    public static class ValidationResult {
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
        private final String holder;
        private final String field;
        private final String rule;
        private final String message;

        public ValidationError(String holder, String field, String rule, String message) {
            this.holder = holder;
            this.field = field;
            this.rule = rule;
            this.message = message;
        }

        public String getHolder() { return field; }
        public String getField() { return field; }
        public String getFullField() {
            if(message == null && holder == null ) return "";

            if((message == null) != (holder == null) )
                return message == null? holder: message;

            return holder + "." + field;
        }

        public String getRule() { return rule; }
        public String getMessage() { return message; }
        public String getFullMessage() { return getFullField() + ": "+ message; }
    }
}
