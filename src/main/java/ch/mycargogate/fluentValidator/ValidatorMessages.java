package ch.mycargogate.fluentValidator;

import lombok.extern.slf4j.Slf4j;

import java.util.ResourceBundle;

@Slf4j
public class ValidatorMessages {
    private static ResourceBundle bundle;

    static {
        bundle = ResourceBundle.getBundle("validation-messages");
    }

    public static String translate(String code, Object ... args) {
        try {
            var m = bundle.getString(code);
            return String.format(m, args);
        } catch(Exception e) {
            log.error("Invalid error code or message format: " + code, e);
            return "INVALID LABEL code=" + code;
        }
    }
}
