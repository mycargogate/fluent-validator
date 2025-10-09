package ch.mycargogate.fluentValidator;

import java.util.List;

class ObjectValidator<F> extends BaseValidator<F> {

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
}
