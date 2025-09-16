
package com.example.validator;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class Validator<T> {

    private final List<FieldRule> fieldRules;
    private final List<ObjectRule<T>> objectRules;

    private Validator(List<FieldRule> fieldRules, List<ObjectRule<T>> objectRules) {
        this.fieldRules = List.copyOf(fieldRules);
        this.objectRules = List.copyOf(objectRules);
    }

    public static <T> ValidatorBuilder<T> builder() {
        return new ValidatorBuilder<>();
    }

    public ValidationResult validate(T object) {
        List<String> errors = new ArrayList<>();

        for (FieldRule rule : fieldRules) {
            Object value = readField(object, rule.fieldName());
            if (value == null) {
                if (rule.mandatory()) errors.add("Field '" + rule.fieldName() + "' is mandatory.");
                continue;
            }

            // Numbers
            if (value instanceof Number number) {
                if (rule.minNumber() != null && number.doubleValue() < rule.minNumber().doubleValue()) {
                    errors.add("Field '" + rule.fieldName() + "' must be ≥ " + rule.minNumber() + ".");
                }
                if (rule.maxNumber() != null && number.doubleValue() > rule.maxNumber().doubleValue()) {
                    errors.add("Field '" + rule.fieldName() + "' must be ≤ " + rule.maxNumber() + ".");
                }
            }

            // Strings
            if (value instanceof CharSequence cs) {
                String s = cs.toString();
                if (Boolean.TRUE.equals(rule.notBlank()) && s.isBlank()) {
                    errors.add("Field '" + rule.fieldName() + "' must not be blank.");
                }
                if (rule.minLength() != null && s.length() < rule.minLength()) {
                    errors.add("Field '" + rule.fieldName() + "' length must be ≥ " + rule.minLength() + ".");
                }
                if (rule.maxLength() != null && s.length() > rule.maxLength()) {
                    errors.add("Field '" + rule.fieldName() + "' length must be ≤ " + rule.maxLength() + ".");
                }
                if (rule.regex() != null && !s.matches(rule.regex())) {
                    errors.add("Field '" + rule.fieldName() + "' does not match regex: " + rule.regex());
                }
            }

            // Enums
            if (value instanceof Enum<?> e) {
                if (rule.enumType() != null && !rule.enumType().isInstance(e)) {
                    errors.add("Field '" + rule.fieldName() + "' must be of enum type " + rule.enumType().getSimpleName() + ".");
                }
                if (rule.allowedEnumNames() != null && !rule.allowedEnumNames().isEmpty()
                        && !rule.allowedEnumNames().contains(e.name())) {
                    errors.add("Field '" + rule.fieldName() + "' must be one of " + rule.allowedEnumNames() + ".");
                }
            }

            // Collections
            if (value instanceof Collection<?> col) {
                if (Boolean.TRUE.equals(rule.collectionNotEmpty()) && col.isEmpty()) {
                    errors.add("Field '" + rule.fieldName() + "' must not be empty.");
                }
                if (rule.collectionMinSize() != null && col.size() < rule.collectionMinSize()) {
                    errors.add("Field '" + rule.fieldName() + "' size must be ≥ " + rule.collectionMinSize() + ".");
                }
                if (rule.collectionMaxSize() != null && col.size() > rule.collectionMaxSize()) {
                    errors.add("Field '" + rule.fieldName() + "' size must be ≤ " + rule.collectionMaxSize() + ".");
                }
            }

            // Dates / Times
            if (value instanceof LocalDate ld) {
                if (rule.localDateNotBefore() != null && ld.isBefore(rule.localDateNotBefore())) {
                    errors.add("Field '" + rule.fieldName() + "' must be on/after " + rule.localDateNotBefore() + ".");
                }
                if (rule.localDateNotAfter() != null && ld.isAfter(rule.localDateNotAfter())) {
                    errors.add("Field '" + rule.fieldName() + "' must be on/before " + rule.localDateNotAfter() + ".");
                }
            }
            if (value instanceof LocalDateTime ldt) {
                if (rule.localDateTimeNotBefore() != null && ldt.isBefore(rule.localDateTimeNotBefore())) {
                    errors.add("Field '" + rule.fieldName() + "' must be on/after " + rule.localDateTimeNotBefore() + ".");
                }
                if (rule.localDateTimeNotAfter() != null && ldt.isAfter(rule.localDateTimeNotAfter())) {
                    errors.add("Field '" + rule.fieldName() + "' must be on/before " + rule.localDateTimeNotAfter() + ".");
                }
            }
            if (value instanceof Instant inst) {
                if (rule.instantNotBefore() != null && inst.isBefore(rule.instantNotBefore())) {
                    errors.add("Field '" + rule.fieldName() + "' must be on/after " + rule.instantNotBefore() + ".");
                }
                if (rule.instantNotAfter() != null && inst.isAfter(rule.instantNotAfter())) {
                    errors.add("Field '" + rule.fieldName() + "' must be on/before " + rule.instantNotAfter() + ".");
                }
            }

            // Custom checks
            if (rule.customChecks() != null && !rule.customChecks().isEmpty()) {
                for (FieldCustomCheck cc : rule.customChecks()) {
                    boolean ok;
                    if (cc.uniPredicate() != null) {
                        ok = cc.uniPredicate().test(value);
                    } else if (cc.biPredicate() != null) {
                        ok = cc.biPredicate().test(object, value);
                    } else ok = true;

                    if (!ok) errors.add(cc.message() != null
                            ? cc.message()
                            : "Field '" + rule.fieldName() + "' failed custom validation.");
                }
            }
        }

        for (ObjectRule<T> r : objectRules) {
            boolean ok = r.predicate().test(object);
            if (!ok) errors.add(r.message());
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }

    // Reflection helper
    private static Object readField(Object target, String name) {
        if (target == null) return null;
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to read field: " + name, e);
            }
        }
        String base = name.substring(0, 1).toUpperCase() + name.substring(1);
        for (String pre : List.of("get", "is")) {
            try {
                return target.getClass().getMethod(pre + base).invoke(target);
            } catch (ReflectiveOperationException ignored) {}
        }
        return null;
    }

    // Fluent API
    public static final class ValidatorBuilder<T> {
        private final List<FieldRule> fieldRules = new ArrayList<>();
        private final List<ObjectRule<T>> objectRules = new ArrayList<>();

        public FieldRuleBuilder<T> fieldRule(String fieldName) {
            Objects.requireNonNull(fieldName, "fieldName");
            return new FieldRuleBuilder<>(this, fieldName);
        }

        public Validator<T> objectRule(Predicate<T> predicate) {
            return objectRule(predicate, "Object rule failed.");
        }

        public Validator<T> objectRule(Predicate<T> predicate, String message) {
            Objects.requireNonNull(predicate, "predicate");
            objectRules.add(new ObjectRule<>(predicate, message));
            return new Validator<>(fieldRules, objectRules);
        }

        public Validator<T> objectCustom(Predicate<T> predicate, String message) {
            return objectRule(predicate, message);
        }

        public ValidatorBuilder<T> objectRuleChain(Predicate<T> predicate) {
            return objectRuleChain(predicate, "Object rule failed.");
        }

        public ValidatorBuilder<T> objectRuleChain(Predicate<T> predicate, String message) {
            Objects.requireNonNull(predicate, "predicate");
            objectRules.add(new ObjectRule<>(predicate, message));
            return this;
        }

        public Validator<T> build() { return new Validator<>(fieldRules, objectRules); }

        public ValidationResult validate(T object) { return build().validate(object); }
    }

    public static final class FieldRuleBuilder<T> {
        private final ValidatorBuilder<T> parent;
        private final String fieldName;

        private FieldRuleBuilder(ValidatorBuilder<T> parent, String fieldName) {
            this.parent = parent;
            this.fieldName = fieldName;
        }

        public ValidatorBuilder<T> mandatory() {
            parent.fieldRules.add(FieldRule.mandatory(fieldName));
            return parent;
        }

        public FieldOptionalBuilder<T> optional() {
            return new FieldOptionalBuilder<>(parent, FieldRule.optional(fieldName));
        }
    }

    public static final class FieldOptionalBuilder<T> {
        private final ValidatorBuilder<T> parent;
        private FieldRule rule;

        private FieldOptionalBuilder(ValidatorBuilder<T> parent, FieldRule base) {
            this.parent = parent;
            this.rule = base;
        }

        // Numeric
        public FieldOptionalBuilder<T> min(Number min) { rule = rule.withMinNumber(min); return this; }
        public FieldOptionalBuilder<T> max(Number max) { rule = rule.withMaxNumber(max); return this; }

        // Strings
        public FieldOptionalBuilder<T> minLength(int n) { rule = rule.withMinLength(n); return this; }
        public FieldOptionalBuilder<T> maxLength(int n) { rule = rule.withMaxLength(n); return this; }
        public FieldOptionalBuilder<T> notBlank()       { rule = rule.withNotBlank(true); return this; }
        public FieldOptionalBuilder<T> regex(String r)  { rule = rule.withRegex(r); return this; }

        // Enums
        public <E extends Enum<E>> FieldOptionalBuilder<T> enumOf(Class<E> enumClass) {
            rule = rule.withEnumType(enumClass); return this;
        }
        @SafeVarargs
        public final <E extends Enum<E>> FieldOptionalBuilder<T> inEnum(E... allowed) {
            Objects.requireNonNull(allowed, "allowed");
            Set<String> names = new LinkedHashSet<>();
            Class<?> type = null;
            for (E e : allowed) {
                if (e == null) continue;
                names.add(e.name());
                type = (type == null) ? e.getDeclaringClass() : type;
            }
            rule = rule.withEnumType(type).withAllowedEnumNames(names);
            return this;
        }

        // Collections
        public FieldOptionalBuilder<T> notEmpty()     { rule = rule.withCollectionNotEmpty(true); return this; }
        public FieldOptionalBuilder<T> minSize(int n) { rule = rule.withCollectionMinSize(n);     return this; }
        public FieldOptionalBuilder<T> maxSize(int n) { rule = rule.withCollectionMaxSize(n);     return this; }

        // Dates / Times
        public FieldOptionalBuilder<T> notBefore(LocalDate d)      { rule = rule.withLocalDateNotBefore(d);      return this; }
        public FieldOptionalBuilder<T> notAfter(LocalDate d)       { rule = rule.withLocalDateNotAfter(d);       return this; }
        public FieldOptionalBuilder<T> notBefore(LocalDateTime dt) { rule = rule.withLocalDateTimeNotBefore(dt); return this; }
        public FieldOptionalBuilder<T> notAfter(LocalDateTime dt)  { rule = rule.withLocalDateTimeNotAfter(dt);  return this; }
        public FieldOptionalBuilder<T> notBefore(Instant i)        { rule = rule.withInstantNotBefore(i);        return this; }
        public FieldOptionalBuilder<T> notAfter(Instant i)         { rule = rule.withInstantNotAfter(i);         return this; }

        // Custom
        public FieldOptionalBuilder<T> custom(Predicate<Object> valuePredicate, String message) {
            rule = rule.withCustomValue(valuePredicate, message);
            return this;
        }
        public FieldOptionalBuilder<T> customWithObject(BiPredicate<T, Object> objectAndValuePredicate, String message) {
            @SuppressWarnings("unchecked")
            BiPredicate<Object,Object> raw = (BiPredicate<Object,Object>)(BiPredicate<?,?>) objectAndValuePredicate;
            rule = rule.withCustomObject(raw, message);
            return this;
        }

        public ValidatorBuilder<T> done() {
            if (!parent.fieldRules.contains(rule)) parent.fieldRules.add(rule);
            return parent;
        }
    }

    // Data holders
    private record FieldCustomCheck(
        Predicate<Object> uniPredicate,
        BiPredicate<Object,Object> biPredicate,
        String message
    ) {}

    private record FieldRule(
        String fieldName,
        boolean mandatory,

        // numbers
        Number minNumber,
        Number maxNumber,

        // strings
        Integer minLength,
        Integer maxLength,
        Boolean notBlank,
        String regex,

        // enums
        Class<?> enumType,
        Set<String> allowedEnumNames,

        // collections
        Integer collectionMinSize,
        Integer collectionMaxSize,
        Boolean collectionNotEmpty,

        // dates/times
        LocalDate      localDateNotBefore,
        LocalDate      localDateNotAfter,
        LocalDateTime  localDateTimeNotBefore,
        LocalDateTime  localDateTimeNotAfter,
        Instant        instantNotBefore,
        Instant        instantNotAfter,

        // custom
        List<FieldCustomCheck> customChecks
    ) {
        static FieldRule mandatory(String name) {
            return new FieldRule(name, true, null,null, null,null,null,null, null,null,
                    null,null,null, null,null,null,null,null,null, new ArrayList<>());
        }
        static FieldRule optional(String name) {
            return new FieldRule(name, false, null,null, null,null,null,null, null,null,
                    null,null,null, null,null,null,null,null,null, new ArrayList<>());
        }

        // numbers
        FieldRule withMinNumber(Number n){ return copy(minNumber, n, true); }
        FieldRule withMaxNumber(Number n){ return copy(maxNumber, n, false); }

        // strings
        FieldRule withMinLength(Integer n){ return new FieldRule(fieldName, mandatory, minNumber, maxNumber, n, maxLength, notBlank, regex, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, customChecksCopy()); }
        FieldRule withMaxLength(Integer n){ return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, n, notBlank, regex, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, customChecksCopy()); }
        FieldRule withNotBlank(Boolean b){ return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, b, regex, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, customChecksCopy()); }
        FieldRule withRegex(String r)    { return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, r, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, customChecksCopy()); }

        // enums
        FieldRule withEnumType(Class<?> c){ return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, c, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, customChecksCopy()); }
        FieldRule withAllowedEnumNames(Set<String> s){ return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, enumType, (s==null?null:Set.copyOf(s)), collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, customChecksCopy()); }

        // collections
        FieldRule withCollectionMinSize(Integer n){ return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, enumType, allowedEnumNames, n, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, customChecksCopy()); }
        FieldRule withCollectionMaxSize(Integer n){ return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, enumType, allowedEnumNames, collectionMinSize, n, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, customChecksCopy()); }
        FieldRule withCollectionNotEmpty(Boolean b){ return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, b, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, customChecksCopy()); }

        // dates/times
        FieldRule withLocalDateNotBefore(LocalDate d){ return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, d, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, customChecksCopy()); }
        FieldRule withLocalDateNotAfter(LocalDate d) { return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, d, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, customChecksCopy()); }
        FieldRule withLocalDateTimeNotBefore(LocalDateTime dt){ return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, dt, localDateTimeNotAfter, instantNotBefore, instantNotAfter, customChecksCopy()); }
        FieldRule withLocalDateTimeNotAfter(LocalDateTime dt) { return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, dt, instantNotBefore, instantNotAfter, customChecksCopy()); }
        FieldRule withInstantNotBefore(Instant i){ return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, i, instantNotAfter, customChecksCopy()); }
        FieldRule withInstantNotAfter(Instant i) { return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, i, customChecksCopy()); }

        // custom
        FieldRule withCustomValue(Predicate<Object> p, String msg){
            List<FieldCustomCheck> list = customChecksCopy();
            list.add(new FieldCustomCheck(p, null, msg));
            return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, list);
        }
        FieldRule withCustomObject(BiPredicate<Object,Object> p, String msg){
            List<FieldCustomCheck> list = customChecksCopy();
            list.add(new FieldCustomCheck(null, p, msg));
            return new FieldRule(fieldName, mandatory, minNumber, maxNumber, minLength, maxLength, notBlank, regex, enumType, allowedEnumNames, collectionMinSize, collectionMaxSize, collectionNotEmpty, localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter, instantNotBefore, instantNotAfter, list);
        }

        private FieldRule copy(Number oldWhich, Number newVal, boolean isMin) {
            return new FieldRule(fieldName, mandatory,
                    isMin ? newVal : minNumber,
                    isMin ? maxNumber : newVal,
                    minLength, maxLength, notBlank, regex,
                    enumType, allowedEnumNames,
                    collectionMinSize, collectionMaxSize, collectionNotEmpty,
                    localDateNotBefore, localDateNotAfter, localDateTimeNotBefore, localDateTimeNotAfter,
                    instantNotBefore, instantNotAfter,
                    customChecksCopy());
        }
        private List<FieldCustomCheck> customChecksCopy() {
            return customChecks == null ? new ArrayList<>() : new ArrayList<>(customChecks);
        }
    }

    private record ObjectRule<T>(Predicate<T> predicate, String message) {}

    public static final class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors == null ? List.of() : List.copyOf(errors);
        }
        public static ValidationResult ok() { return new ValidationResult(true, List.of()); }
        public static ValidationResult fail(List<String> errors) { return new ValidationResult(false, errors); }
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        @Override public String toString() { return valid ? "ValidationResult{OK}" : "ValidationResult{errors=" + errors + "}"; }
    }
}
