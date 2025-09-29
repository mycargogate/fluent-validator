package ch.mycargogate.fluentValidator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldNameTest {
    public static class Person {
        private String firstName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
    }

    @Test
    public void test() {
        var fieldName = FieldName.nameOf(Person::getFirstName);
        assertEquals("firstName", fieldName);
    }
}
