package ch.mycargogate.fluentValidator;

// ==== ValidationError ====
public class ValidationError {
    private final String holder;
    private final String field;
    private final String rule;
    private final String message;

    public ValidationError(String holder, String field, String rule, String message) {
        this.holder = holder;
        this.field = field;
        this.rule = rule;
        this.message = message;
    }

    public String getHolder() {
        return field;
    }

    public String getField() {
        return field;
    }

    public String getFullField() {
        if (message == null && holder == null) return "";

        if ((message == null) != (holder == null))
            return message == null ? holder : message;

        return holder + "." + field;
    }

    public String getRule() {
        return rule;
    }

    public String getMessage() {
        return message;
    }

    public String getFullMessage() {
        return getFullField() + ": " + message;
    }
}
