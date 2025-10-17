package ch.mycargogate.fluentValidator;

import lombok.Getter;

import java.util.*;
import java.util.function.Predicate;

/**
 * Registry API for custom reusable validators.
 */
public abstract class CustomRegistry {

    private final Map<Class<?>, Set<RegisteredFluentValidator<?>>> validatorRegistry = new HashMap<>();

    public <T> void register(Class<T> clazz, String holderRegex, FluentValidator<T> validator, Map<String, String> properties) {

        var registrationsForClass = validatorRegistry.computeIfAbsent(clazz, k -> new HashSet<>());
        //var registration = new RegisteredFluentValidator<>(clazz, holderRegex, new HashMap<String, String>(properties));
        //registrationsForClass.add(registration);

        // Super classes validators may apply to this class as well
        for(Class<?> c = clazz.getSuperclass(); c != null; c = c.getSuperclass()) {
            var superValidator = validatorRegistry.get(c);
            if(superValidator != null)
                registrationsForClass.addAll(superValidator);
        }

        // This validator may apply to subclasses
        for(Class<?> c : validatorRegistry.keySet()) {
            if(c != clazz && c.isAssignableFrom(clazz)) {
                validatorRegistry.get(c).addAll(registrationsForClass);
            }
        }
    }

    public <T> Collection<FluentValidator<T>> findValidators(Class<T> clazz, String holder, Map<String, String> properties) {

        var registered = validatorRegistry.get(clazz);
        var result = new ArrayList<FluentValidator<?>>();

        for (var registeredFluentValidator : registered) {
            if( matchRegistered(registeredFluentValidator, clazz, holder, properties) )
                result.add(registeredFluentValidator.validator());
        }

        @SuppressWarnings("unchecked")
        var resultCast = (Collection<FluentValidator<T>>)(Object) result;

        return resultCast;
    }

    private <T> boolean matchRegistered(RegisteredFluentValidator<?> reg, Class<T> clazz, String holder, Map<String, String> properties) {
        if(reg.clazz() != clazz || !holder.matches(reg.holderRegex()))
            return false;

        for(var entry: reg.properties().entrySet()) {
            var value = properties.get(entry.getKey());
            if( value == null ) return false;
            if(!entry.getValue().equals(value)) return false;
        }

        return true;
    }

    abstract void register(String name, Predicate<Object> predicate, String message);

    abstract RuleEntry get(String name);

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
