package ch.mycargogate.fluentValidator;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class FluentValidatorCoreTest {
    static class Address {
        private String line;
        private String city;
        private String zipcode;

        public String getLine() {
            return line;
        }

        public void setLine(String line) {
            this.line = line;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZipcode() {
            return zipcode;
        }

        public void setZipcode(String zipcode) {
            this.zipcode = zipcode;
        }
    }

    static class User {
        String name;
        Integer age;
        Double score;
        String status;
        Role role;
        Set<String> tags;
        LocalDate date;
        String email;

        Address address = new Address();

        // Optional getter demo (not required for reflection lookup by field name)
        public Integer getAge() { return age; }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public Set<String> getTags() {
            return tags;
        }

        public void setTags(Set<String> tags) {
            this.tags = tags;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }


    enum Role { ADMIN, USER }

    @Test
    void mandatory_and_optional_rules() {
        FluentValidator<User> fluentValidator = FluentValidator.<User>builder()
                .fieldRule(User::getName).mandatory().done()
                .fieldRule(User::getEmail).optional().regex(".+@.+\\..+").done()
                .build();

        User u = new User();
        u.name = null; // mandatory should fail
        u.email = null; // optional with null should be fine

        ValidationResult result = fluentValidator.validate(u);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals(ErrorCode.MISSING_VALUE)));
        // ensure only name triggered, not email
        assertTrue(result.getErrors().stream().allMatch(e -> "name".equals(e.getField())));
    }

    @Test
    void numeric_min_max_rules() {
        FluentValidator<User> fluentValidator = FluentValidator.<User>builder()
                .fieldRule(User::getAge).min(0).max(120).done()
                .fieldRule(User::getScore).min(0).max(100).done()
                .build();

        User u = new User();
        u.age = -1;     // violates min
        u.score = 150d; // violates max

        var result = fluentValidator.validate(u);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getField().equals("age") && e.getCode().equals(ErrorCode.LOWER_THAN_MIN)));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getField().equals("score") && e.getCode().equals(ErrorCode.GREATER_THAN_MAX)));
    }

    @Test
    void extendsValidator() {

        class A {
            private String a;

            public String getA() {
                return a;
            }
        }
        class B extends A {
            private String b;

            public String getB() {
                return b;
            }
        }

        var b = new B();

        var validatorA = FluentValidator.<A>builder()
                .fieldRule(A::getA).mandatory().done()
                .build();

        var validatorB = FluentValidator.<B>builder()
                .extendsValidator(validatorA)
                .fieldRule(B::getB).mandatory().done()
                .build();

        ValidationResult result = validatorB.validate(b);
        // result.getErrors().forEach(System.out::println);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().filter(e -> e.getField().equals("a")).anyMatch(e -> e.getCode().equals(ErrorCode.MISSING_VALUE)));
        assertTrue(result.getErrors().stream().filter(e -> e.getField().equals("b")).anyMatch(e -> e.getCode().equals(ErrorCode.MISSING_VALUE)));
    }

    @Test
    void collectionValidators() {

        class A {
            private String a;

            public String getA() {
                return a;
            }
        }

        class B  {
            private List<A> listOfAs = Arrays.asList(new A(), new A());

            public List<A> getListOfAs() {
                return listOfAs;
            }
        }

        var b = new B();

        var validatorA = FluentValidator.<A>builder()
                .fieldRule(A::getA).mandatory().done()
                .build();

        var validatorB = FluentValidator.<B>builder()
                .collectionRule(B::getListOfAs).mandatory().elementValidator(validatorA).done()
                .build();

        ValidationResult result = validatorB.validate(b);
        result.getErrors().forEach(System.out::println);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().filter(e -> e.getField().equals("a")).anyMatch(e -> e.getCode().equals(ErrorCode.MISSING_VALUE)));
    }

    @Test
    void validator() {
        var addressValidator = FluentValidator.<Address>builder()
                .fieldRule(Address::getLine).mandatory().done()
                .fieldRule(Address::getCity).mandatory().done()
                .fieldRule(Address::getZipcode).mandatory().done()
                .build();

        FluentValidator<User> fluentValidator = FluentValidator.<User>builder()
                .fieldRule(User::getAddress).validate(addressValidator).done()
                .build();

        Address a = new Address();
        a.city = "Leh";
        a.line = "boudha street";
        a.zipcode = "00000";

        User u = new User();
        u.age = -1;     // violates min
        u.score = 150d; // violates max
        u.address = a;

        var result = fluentValidator.validate(u);
        if(result.getErrors() != null)
            result.getErrors().forEach(System.out::println);
        assertTrue(result.isValid());
    }

    @Test
    void string_length_regex_and_notBlank_rules() {
        FluentValidator<User> fluentValidator = FluentValidator.<User>builder()
                .fieldRule(User::getName).notBlank().minLength(3).maxLength(5).regex("A.*").done()
                .build();

        // 1) blank
        User u1 = new User();
        u1.name = "  ";
        var r1 = fluentValidator.validate(u1);
        assertFalse(r1.isValid());
        assertTrue(r1.getErrors().stream().anyMatch(e -> e.getCode().equals(ErrorCode.NOT_BLANK)));

        // 2) too short
        User u2 = new User();
        u2.name = "Al";
        var r2 = fluentValidator.validate(u2);
        assertFalse(r2.isValid());
        assertTrue(r2.getErrors().stream().anyMatch(e -> e.getCode().equals(ErrorCode.LENGTH_LOWER_THAN)));

        // 3) too long
        User u3 = new User();
        u3.name = "Albion";
        var r3 = fluentValidator.validate(u3);
        assertFalse(r3.isValid());
        assertTrue(r3.getErrors().stream().anyMatch(e -> e.getCode().equals(ErrorCode.LENGTH_GREATER_THAN)));

        // 4) regex mismatch
        User u4 = new User();
        u4.name = "Bob";
        var r4 = fluentValidator.validate(u4);
        assertFalse(r4.isValid());
        assertTrue(r4.getErrors().stream().anyMatch(e -> e.getCode().equals(ErrorCode.REGEX_DONT_MATCH)));
    }

    @Test
    void enum_rules_for_string_and_enum_fields() {
        FluentValidator<User> fluentValidator = FluentValidator.<User>builder()
                .fieldRule(User::getStatus).inEnum("NEW", "DONE").done()
                .fieldRule(User::getRole).inEnum("ADMIN").done()
                .build();

        User u = new User();
        u.status = "PENDING";    // not in [NEW, DONE]
        u.role = Role.USER;       // not ADMIN

        var result = fluentValidator.validate(u);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getField().equals("status") && e.getCode().equals(ErrorCode.MUST_BE_ONE_OF)));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getField().equals("role") && e.getCode().equals(ErrorCode.MUST_BE_ONE_OF)));
    }

    @Test
    void collection_size_rules() {
        FluentValidator<User> fluentValidator = FluentValidator.<User>builder()
                .collectionRule(User::getTags).minSize(2).maxSize(3).done()
                .build();

        User u1 = new User();
        u1.tags = Set.of(); // too small
        var r1 = fluentValidator.validate(u1);
        assertFalse(r1.isValid());
        assertTrue(r1.getErrors().stream().anyMatch(e -> e.getCode().equals(ErrorCode.SIZE_LT)));

        User u2 = new User();
        u2.tags = Set.of("a", "b", "c", "d"); // too large
        var r2 = fluentValidator.validate(u2);
        assertFalse(r2.isValid());
        assertTrue(r2.getErrors().stream().anyMatch(e -> e.getCode().equals(ErrorCode.SIZE_GT)));
    }

    @Test
    void date_range_rules() {
        FluentValidator<User> fluentValidator = FluentValidator.<User>builder()
                .fieldRule(User::getDate).notBefore(LocalDate.of(2020, 1, 1)).notAfter(LocalDate.of(2020, 12, 31))
                .done()
                .build();

        User u1 = new User();
        u1.date = LocalDate.of(2019, 12, 31);
        var r1 = fluentValidator.validate(u1);
        assertFalse(r1.isValid());
        assertTrue(r1.getErrors().stream().anyMatch(e -> e.getCode().equals(ErrorCode.DATE_BEFORE)));

        User u2 = new User();
        u2.date = LocalDate.of(2021, 1, 1);
        var r2 = fluentValidator.validate(u2);
        assertFalse(r2.isValid());
        assertTrue(r2.getErrors().stream().anyMatch(e -> e.getCode().equals(ErrorCode.DATE_AFTER)));
    }

    @Test
    void custom_predicate_rule() {
        FluentValidator<User> fluentValidator = FluentValidator.<User>builder()
                .fieldRule(User::getEmail)
                .predicate(s -> s != null && s.contains("@"), "EMAIL_FORMAT")
                .done()
                .build();

        User u = new User();
        u.email = "invalid";
        var result = fluentValidator.validate(u);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getField().equals("email") && e.getCode().equals("EMAIL_FORMAT") && e.getMessage().contains("must contain @")));
    }

    @Test
    void object_rule() {

        String CODE = "MUST_BE_ADULT";
        FluentValidator<User> fluentValidator = FluentValidator.<User>builder()
                .objectRule(u -> u.age != null && u.age >= 18, CODE)
                .build();

        User u = new User();
        u.age = 16;

        var result = fluentValidator.validate(u);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getField() == null && e.getCode().equals(CODE)));
    }
}
