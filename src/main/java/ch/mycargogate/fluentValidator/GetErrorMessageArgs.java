package ch.mycargogate.fluentValidator;

@FunctionalInterface
public interface GetErrorMessageArgs<T> extends java.io.Serializable {
    Object[] apply(T t);
}