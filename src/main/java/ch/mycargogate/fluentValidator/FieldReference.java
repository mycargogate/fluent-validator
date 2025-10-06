package ch.mycargogate.fluentValidator;

@FunctionalInterface
public interface FieldReference<T, R> extends java.io.Serializable {
    R apply(T t);
}
