package ch.mycargogate.fluentValidator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FluentValidatorReaderJsonTest {

    static class User {
        String name;
        Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

    //@Test
    void testLoadValidatorFromJSON() {

        @SuppressWarnings("unchecked")
        FluentValidator<User> fluentValidator = (FluentValidator<User>)new ValidatorReaderJson().fromResource("validator.json");

        User u = new User();
        u.age = -5; // violates min=0

        ValidationResult result = fluentValidator.validate(u);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getCode().equals(ErrorCode.LOWER_THAN_MIN)));
    }

    //@Test
    void testMissingJSONResource() {
        assertThrows(Exception.class, () ->
                new ValidatorReaderJson().fromResource("missing.json")
        );
    }
}
