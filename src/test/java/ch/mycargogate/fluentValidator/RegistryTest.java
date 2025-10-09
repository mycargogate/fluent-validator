package ch.mycargogate.fluentValidator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RegistryTest {

    static class User {
        String email;
        Integer age;

        public String getEmail() {
            return email;
        }

        public Integer getAge() {
            return age;
        }
    }

    @Test
    void testDefaultRegistryBuiltInRules() {
        DefaultRegistry<Object> registry = new DefaultRegistry<>();
        registry.register("email", v -> v instanceof String s && s.matches("^[^@]+@[^@]+\\.[^@]+$"),
                "EMAIL_FORMAT");

        FluentValidator<User> fluentValidator = FluentValidator.<User>builder()
                .fieldRule(User::getEmail)
                .predicate(registry.get("email").getPredicate(String.class),
                        registry.get("email").getCode())
                .done()
                .build();

        User u = new User();
        u.email = "invalid";  // not a valid email
        u.age = -5;           // not positive

        ValidationResult result = fluentValidator.validate(u);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("EMAIL_FORMAT")));
    }

    @Test
    void testDynamicRegistryRule() {
        DefaultRegistry<Object> registry = new DefaultRegistry<>();
        registry.register("even", v -> v instanceof Integer i && i % 2 == 0,
                "MUST_BE_EVEN");

        FluentValidator<User> fluentValidator = FluentValidator.<User>builder()
                .fieldRule(User::getAge)
                .predicate(registry.get("even").getPredicate(Integer.class),
                        registry.get("even").getCode())
                .done()
                .build();

        User u = new User();
        u.age = 3; // odd, should fail

        ValidationResult result = fluentValidator.validate(u);

        assertFalse(result.isValid());
        assertEquals("MUST_BE_EVEN", result.getErrors().get(0).getCode());
    }
}
