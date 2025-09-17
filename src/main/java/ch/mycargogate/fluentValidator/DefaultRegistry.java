package ch.mycargogate.fluentValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Default registry with common reusable validators.
 */
public class DefaultRegistry<T> implements CustomRegistry<T> {

    private final Map<String, RuleEntry> rules = new HashMap<>();

    public DefaultRegistry() {
        // preload some common rules
        register("notBlank", v -> v instanceof String s && !s.trim().isEmpty(),
                "Field must not be blank");
        register("positive", v -> v instanceof Number n && n.doubleValue() > 0,
                "Field must be positive");
        register("email", v -> v instanceof String s && s.matches("^[^@]+@[^@]+\\.[^@]+$"),
                "Field must be a valid email");
    }

    @Override
    public void register(String name, Predicate<Object> predicate, String message) {
        rules.put(name, new RuleEntry(predicate, message));
    }

    @Override
    public RuleEntry get(String name) {
        return rules.get(name);
    }
}
