package ch.mycargogate.fluentValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

// UNDER WORK

/**
 * Default registry with common reusable validators.
 */
public class DefaultRegistry extends CustomRegistry {

    private final Map<String, RuleEntry> rules = new HashMap<>();

    public DefaultRegistry() {
    }

    @Override
    public void register(String name, Predicate<Object> predicate, String code) {
        rules.put(name, new RuleEntry(predicate, code));
    }

    @Override
    public RuleEntry get(String name) {
        return rules.get(name);
    }
}
