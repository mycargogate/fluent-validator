package ch.mycargogate.fluentValidator;

import java.util.Map;

public record RegisteredFluentValidator<T>(Class<T> clazz, FluentValidator<T> validator, String holderRegex, Map<String, String> properties) {
}
