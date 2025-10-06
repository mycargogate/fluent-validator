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

        Validator<User> validator = Validator.<User>builder()
                .fieldRule(User::getEmail)
                .custom(registry.get("email").getPredicate(String.class),
                        registry.get("email").getMessage())
                .done()
                .fieldRule(User::getAge)
                .custom(registry.get("positive").getPredicate(Integer.class),
                        registry.get("positive").getMessage())
                .done()
                .build();

        User u = new User();
        u.email = "invalid";  // not a valid email
        u.age = -5;           // not positive

        ValidationResult result = validator.validate(u);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getRule().equals("custom")));
    }

    @Test
    void testDynamicRegistryRule() {
        DefaultRegistry<Object> registry = new DefaultRegistry<>();
        registry.register("even", v -> v instanceof Integer i && i % 2 == 0,
                "Must be an even number");

        Validator<User> validator = Validator.<User>builder()
                .fieldRule(User::getAge)
                .custom(registry.get("even").getPredicate(Integer.class),
                        registry.get("even").getMessage())
                .done()
                .build();

        User u = new User();
        u.age = 3; // odd, should fail

        ValidationResult result = validator.validate(u);

        assertFalse(result.isValid());
        assertEquals("Must be an even number",
                result.getErrors().get(0).getMessage());
    }
}
