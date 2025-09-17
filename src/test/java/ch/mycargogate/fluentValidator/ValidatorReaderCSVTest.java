package ch.mycargogate.fluentValidator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ValidatorReaderCSVTest {

    static class User {
        String name;
        Integer age;
    }

    @Test
    void testLoadValidatorFromCSV() {
        DefaultRegistry<User> registry = new DefaultRegistry<>();
        Validator<User> validator = new ValidatorReaderCSV<User>(registry)
                .fromResource("validator.csv");

        User u = new User();
        u.age = -5; // violates min=0

        Validator.ValidationResult result = validator.validate(u);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getRule().equals("min")));
    }

    @Test
    void testMissingCSVResource() {
        DefaultRegistry<User> registry = new DefaultRegistry<>();

        assertThrows(RuntimeException.class, () ->
                new ValidatorReaderCSV<User>(registry).fromResource("missing.csv")
        );
    }
}
