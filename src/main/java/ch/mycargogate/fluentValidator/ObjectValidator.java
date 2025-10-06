package ch.mycargogate.fluentValidator;

import java.util.List;

@FunctionalInterface
public interface ObjectValidator<T> extends java.io.Serializable {
    List<ValidationError> validate(Object value);
}
