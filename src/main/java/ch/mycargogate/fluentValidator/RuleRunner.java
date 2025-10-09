package ch.mycargogate.fluentValidator;

import java.util.List;

@FunctionalInterface
public interface RuleRunner<T> extends java.io.Serializable {
    List<ErrorCodeMessage> run(String holder, T value);
}
