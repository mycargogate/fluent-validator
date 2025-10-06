package ch.mycargogate.fluentValidator;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Predicate;

// ==== ListRule ====
class CollectionRule<E> extends ValueRule<Collection<E>> {
    private Integer minSize, maxSize;
    private Validator<E> elementValidator;

    CollectionRule(Method getter) {
        super(getter);
    }

    CollectionRule(Class<Collection<E>> clazz, String fieldName) {
        super(clazz, fieldName);
    }

    @Override
    void doValidate(String holder, Collection<E> collection, Collection<ValidationError> errors) {

        super.doValidate(holder, collection, errors);

        if (minSize != null && collection.size() < minSize)
            errors.add(new ValidationError(holder, getFieldName(), "minSize", "size must be ≥ " + minSize));

        if (maxSize != null && collection.size() > maxSize)
            errors.add(new ValidationError(holder, getFieldName(), "maxSize", "size must be ≤ " + maxSize));

        if (elementValidator != null) {
            int index = 0;
            for (E e : collection) {
                var result = elementValidator.validate(holder, e, getFieldName(), index);
                errors.addAll(result.getErrors());
                index++;
            }
        }
    }

    public Integer getMinSize() {
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        this.minSize = minSize;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    public Validator<E> getElementValidator() {
        return elementValidator;
    }

    public void setElementValidator(Validator<E> elementValidator) {
        this.elementValidator = elementValidator;
    }

    public static class Builder<T, E> {
        private final CollectionRule<E> rule;
        private final Validator.Builder<T> parent;

        Builder(CollectionRule<E> rule, Validator.Builder<T> parent) {
            this.rule = rule;
            this.parent = parent;
        }

        public Builder<T, E> mandatory() {
            rule.setMandatory(true);
            return this;
        }

        public Builder<T, E> optional() {
            rule.setMandatory(false);
            return this;
        }

        public Builder<T, E> minSize(int size) {
            rule.setMinSize(size);
            return this;
        }

        public Builder<T, E> maxSize(int size) {
            rule.setMaxSize(size);
            return this;
        }

        public Builder<T, E> elementValidator(Validator<E> elementValidator) {
            rule.setElementValidator(elementValidator);
            return this;
        }

        public Builder<T, E> custom(Predicate<T> predicateErase, String message) {

            @SuppressWarnings("unchecked")
            var predicate = (Predicate<Object>) predicateErase;
            rule.getCustomRules().add(new ValueRule.CustomRule(predicate, message));

            return this;
        }

        public Validator.Builder<T> done() {
            return parent;
        }
    }
}
