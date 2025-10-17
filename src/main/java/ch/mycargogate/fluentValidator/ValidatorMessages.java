package ch.mycargogate.fluentValidator;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

@Slf4j
public class ValidatorMessages {
    private static Properties properties;

    static {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources("fluent-validator-messages.properties");

            properties = new Properties();

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                System.out.println("Loading: " + resource);

                try (InputStream is = resource.openStream()) {
                    Properties props = new Properties();
                    props.load(is);
                    properties.putAll(props); // merge properties
                }
            }

            if(properties.isEmpty())
                log.error("Cannot find fluent validator messages from files named fluent-validator-messages.properties");

        } catch(Exception e) {
            log.error("Cannot load the fluent validator messages", e);
        }
    }

    public static String message(String code, Object ... args) {
        var m = properties.getProperty(code);

        if( m == null ) {
            log.error("Cannot find error message for the code " + code);
            return "INVALID LABEL code=" + code;
        }

        try {
                return String.format(m, args);
        } catch(Exception e) {
            log.error("Invalid message format: code=" + code + ", message="+ m, e);
            return "INVALID LABEL code=" + code;
        }
    }
}
