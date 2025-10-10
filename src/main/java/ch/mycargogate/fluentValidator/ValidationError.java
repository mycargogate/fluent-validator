package ch.mycargogate.fluentValidator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

// ==== ValidationError ====
@Getter
@RequiredArgsConstructor
@ToString
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

    public String getFullField() {
        if (message == null && holder == null) return "";

        if ((message == null) != (holder == null))
            return message == null ? holder : message;

        return holder + "." + field;
    }

    public String getFullMessage() {
        return getFullField() + ": " + message;
    }
}
