
package com.example.validator.csv;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** A simple map-backed CustomRegistry. */
public class DefaultRegistry<T> implements CustomRegistry<T> {
    private final Map<String, Predicate<Object>> valueMap = new HashMap<>();
    private final Map<String, BiPredicate<T,Object>> objValMap = new HashMap<>();
    private final Map<String, Predicate<T>> objectMap = new HashMap<>();

    public DefaultRegistry<T> addValue(String name, Predicate<Object> p) {
        valueMap.put(Objects.requireNonNull(name), Objects.requireNonNull(p));
        return this;
    }
    public DefaultRegistry<T> addObjectAndValue(String name, BiPredicate<T,Object> p) {
        objValMap.put(Objects.requireNonNull(name), Objects.requireNonNull(p));
        return this;
    }
    public DefaultRegistry<T> addObject(String name, Predicate<T> p) {
        objectMap.put(Objects.requireNonNull(name), Objects.requireNonNull(p));
        return this;
    }

    @Override public Predicate<Object> valuePredicate(String name) { return valueMap.get(name); }
    @Override public BiPredicate<T,Object> objectAndValuePredicate(String name) { return objValMap.get(name); }
    @Override public Predicate<T> objectPredicate(String name) { return objectMap.get(name); }
}
