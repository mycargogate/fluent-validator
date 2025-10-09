package ch.mycargogate.fluentValidator;

// ==== ValidationError ====
public class ValidationError {

    private final String holder;
    private final String field;
    private final String code;
    private final String message;

    public ValidationError(String holder, String field, ErrorCodeMessage ecm) {
        this.holder = holder;
        this.field = field;
        this.code = ecm.code();
        this.message = ecm.message();
    }

    public ValidationError(String holder, String field, String code, String message) {
        this.holder = holder;
        this.field = field;
        this.code = code;
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

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getFullMessage() {
        return getFullField() + ": " + message;
    }
}
