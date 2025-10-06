package ch.mycargogate.fluentValidator;

import java.util.List;

@FunctionalInterface
public interface FieldValidator<T> extends java.io.Serializable {
    List<ValidationError> validate(String fieldName, Object value);
}
