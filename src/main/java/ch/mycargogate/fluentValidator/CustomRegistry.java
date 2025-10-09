package ch.mycargogate.fluentValidator;

import lombok.Getter;

import java.util.function.Predicate;

/**
 * Registry API for custom reusable validators.
 */
public interface CustomRegistry<T> {
    /**
     * Register a new rule under a given name.
     *
     * @param name rule name (unique)
     * @param predicate rule implementation
     * @param message default error message
     */
    void register(String name, Predicate<Object> predicate, String message);

    /**
     * Retrieve a rule by name.
     *
     * @param name rule name
     * @return entry containing predicate and default message, or null if not found
     */
    RuleEntry get(String name);

    class RuleEntry {
        private final Predicate<Object> predicate;

        @Getter
        private final String code;

        public RuleEntry(Predicate<Object> predicate, String code) {
            this.predicate = predicate;
            this.code = code;
        }

        public <T> Predicate<T> getPredicate(Class<T> clazz) {
            //noinspection unchecked
            return (Predicate<T>)predicate;
        }
    }
}
