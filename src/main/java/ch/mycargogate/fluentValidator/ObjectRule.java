package ch.mycargogate.fluentValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// ==== ObjectRule ====
class ObjectRule<T> extends ValidationRule {
    private final Predicate<T> predicate;
    private final String message;
    private final List<ObjectValidator<T>> lambdas = new ArrayList<>();

    ObjectRule(Predicate<T> predicate, String message) {
        this.predicate = predicate;
        this.message = message;
    }

    public Predicate<T> getPredicate() {
        return predicate;
    }

    public String getMessage() {
        return message;
    }

    public List<ObjectValidator<T>> getLambdas() {
        return lambdas;
    }
}
