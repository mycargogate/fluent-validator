
package com.example.validator.csv;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Provides named lambdas the CSV can reference. */
public interface CustomRegistry<T> {
    Predicate<Object> valuePredicate(String name);
    BiPredicate<T, Object> objectAndValuePredicate(String name);
    Predicate<T> objectPredicate(String name);
}
