package ch.mycargogate.fluentValidator;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

// ==== ListRule ====
@Getter
@Setter
class CollectionValidator<E> extends ValueValidator<Collection<E>> {
    private Integer minSize, maxSize;
    private FluentValidator<E> elementFluentValidator;
    private final List<RuleRunner<E>> elementRules = new ArrayList<>();

    CollectionValidator(Method getter) {
        super(getter);
    }

    CollectionValidator(Class<Collection<E>> clazz, String fieldName) {
        super(clazz, fieldName);
    }

    private void addElementRule(RuleRunner<E> rule) {
        elementRules.add(rule);
    }

    private void addElementPredicate(Predicate<E> predicate, String code) {
        var elementRule = new RuleRunner<E>() {

            @Override
            public List<ErrorCodeMessage> run(String holder, E value) {
                if( ! predicate.test(value) ) {
                    String message = String.format(ValidatorMessages.message(code, getFullFieldName(holder), value));
                    return Collections.singletonList(new ErrorCodeMessage(code, message));
                }
                return null;
            }
        };

        addElementRule(elementRule);
    }

    protected void validate(String holder, Object object, List<ValidationError> errors) {
        try {
            @SuppressWarnings("unchecked")
            Collection<E> value = (Collection<E>) getGetter((Class<E>) object.getClass()).invoke(object);

            doValidate(holder, value, errors);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doValidate(String holder, Collection<E> collection, List<ValidationError> errors) {
        super.doValidate(holder, collection, errors);

        // optional null value
        if(collection == null ) return;

        if (minSize != null && collection.size() < minSize) {
            String message = ValidatorMessages.message(ErrorCode.SIZE_LT, getFullFieldName(holder), collection.size(), minSize);
            addErrorMessage(holder, errors, ErrorCode.SIZE_LT, message);
        }

        if (maxSize != null && collection.size() > maxSize) {
            String message = ValidatorMessages.message(ErrorCode.SIZE_GT, getFullFieldName(holder), collection.size(), maxSize);
            addErrorMessage(holder, errors, ErrorCode.SIZE_GT, message);
        }

        if (elementFluentValidator != null) {
            var newHolder = getFullFieldName(holder);

            int index = 0;
            for (E e : collection) {

                String elementHolder = holder == null? e.getClass().getSimpleName(): holder;
                if( e instanceof HolderNode node)
                    elementHolder += "." +getFieldName() + "[" + node.holderNodeName() + "]";
                else
                    elementHolder +=  "." + getFieldName();

                var result = elementFluentValidator.validate(elementHolder, e);
                errors.addAll(result.getErrors());
                index++;
            }
        }
    }

    public FluentValidator<E> getElementValidator() {
        return elementFluentValidator;
    }

    public void setElementValidator(FluentValidator<E> elementFluentValidator) {
        this.elementFluentValidator = elementFluentValidator;
    }

    public static class Builder<T, E> {
        private final CollectionValidator<E> rule;
        private final FluentValidator.Builder<T> parent;

        Builder(CollectionValidator<E> rule, FluentValidator.Builder<T> parent) {
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

        public Builder<T, E> elementValidator(FluentValidator<E> elementFluentValidator) {
            rule.setElementValidator(elementFluentValidator);
            return this;
        }

        public Builder<T, E> elementRule(RuleRunner<E> elementRule) {
            rule.addElementRule(elementRule);
            return this;
        }

        public Builder<T, E> elementPredicate(Predicate<E> predicate, String code) {
            rule.addElementPredicate(predicate, code);
            return this;
        }

        public FluentValidator.Builder<T> done() {
            return parent;
        }
    }
}
