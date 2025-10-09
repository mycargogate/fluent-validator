package ch.mycargogate.fluentValidator;

@FunctionalInterface
public interface GetterRef<T, R> extends java.io.Serializable {
    R apply(T t);
}
