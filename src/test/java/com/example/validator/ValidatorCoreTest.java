package com.example.validator;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorCoreTest {

    enum Status { ACTIVE, PAUSED, DISABLED }

    static class Sample {
        Integer age;
        String  name;
        Status  status;
        List<String> tags;
        LocalDate startDate;
        LocalDate endDate;
        LocalDateTime createdAt;
        Instant expiresAt;
        boolean vip;
        double  discount;

        Sample(Integer age, String name, Status status, List<String> tags,
               LocalDate startDate, LocalDate endDate,
               LocalDateTime createdAt, Instant expiresAt,
               boolean vip, double discount) {
            this.age = age;
            this.name = name;
            this.status = status;
            this.tags = tags;
            this.startDate = startDate;
            this.endDate = endDate;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.vip = vip;
            this.discount = discount;
        }
    }

    @Test
    void validObjectPassesAllRules() {
        var v = Validator.<Sample>builder()
                .fieldRule("name").mandatory()
                .fieldRule("age").optional().min(0).max(130)
                .custom(vv -> ((Number)vv).intValue() % 2 == 0, "age must be even").done()
                .fieldRule("status").optional().inEnum(Status.ACTIVE, Status.PAUSED).done()
                .fieldRule("tags").optional().notEmpty().minSize(1).maxSize(5).done()
                .fieldRule("startDate").optional().notBefore(LocalDate.of(2024,1,1)).done()
                .fieldRule("endDate").optional().notAfter(LocalDate.of(2026,12,31)).done()
                .fieldRule("createdAt").optional().notAfter(LocalDateTime.of(2030,1,1,0,0)).done()
                .fieldRule("expiresAt").optional().notBefore(Instant.parse("2024-01-01T00:00:00Z")).done()
                .fieldRule("discount").optional()
                .customWithObject((s, val) -> s.vip || ((Number)val).doubleValue() <= 0.10,
                        "non-VIP discount must be ≤ 10%")
                .done()
                .objectRule(s -> s.endDate == null || !s.endDate.isBefore(s.startDate),
                        "endDate must be on/after startDate")
                ;

        var ok = new Sample(
                22,                              // age (even)
                "Alice",                         // name (mandatory)
                Status.ACTIVE,                   // status (allowed)
                List.of("a","b"),                // tags
                LocalDate.of(2025,1,10),         // startDate
                LocalDate.of(2025,1,10),         // endDate
                LocalDateTime.of(2025,1,1,12,0), // createdAt
                Instant.parse("2025-06-01T00:00:00Z"), // expiresAt
                true,                            // vip
                0.25                             // discount (ok because vip)
        );

        var result = v.validate(ok);
        assertTrue(result.isValid(), result.toString());
    }

    @Test
    void missingMandatoryAndNumericViolationsAreReported() {
        var v = Validator.<Sample>builder()
                .fieldRule("name").mandatory()
                .fieldRule("age").optional().min(10).max(20).done()
                .build();

        var bad = new Sample(
                9,         // age below min
                null,      // name missing (mandatory)
                null, List.of(), null, null, null, null, false, 0.0
        );

        var r = v.validate(bad);
        assertFalse(r.isValid());
        assertTrue(r.getErrors().stream().anyMatch(e -> e.contains("mandatory")), r.toString());
        assertTrue(r.getErrors().stream().anyMatch(e -> e.contains("≥ 10")), r.toString());
    }

    @Test
    void stringAndRegexConstraints() {
        var v = Validator.<Sample>builder()
                .fieldRule("name").optional().notBlank().minLength(2).maxLength(5).regex("[A-Za-z ]+").done()
                .build();

        var bad = new Sample(30, " ", null, null, null, null, null, null, false, 0.0);
        var r1 = v.validate(bad);
        assertFalse(r1.isValid());
        assertTrue(r1.getErrors().stream().anyMatch(e -> e.contains("must not be blank")));

        var bad2 = new Sample(30, "A1", null, null, null, null, null, null, false, 0.0);
        var r2 = v.validate(bad2);
        assertFalse(r2.isValid());
        assertTrue(r2.getErrors().stream().anyMatch(e -> e.contains("does not match regex")));
    }

    @Test
    void enumCollectionAndDates() {
        var v = Validator.<Sample>builder()
                .fieldRule("status").optional().inEnum(Status.ACTIVE, Status.PAUSED).done()
                .fieldRule("tags").optional().notEmpty().minSize(2).maxSize(3).done()
                .fieldRule("startDate").optional().notBefore(LocalDate.of(2024,1,1)).done()
                .fieldRule("endDate").optional().notAfter(LocalDate.of(2026,12,31)).done()
                .objectRule(s -> s.endDate == null || !s.endDate.isBefore(s.startDate),
                        "endDate must be on/after startDate")
                ;

        // wrong enum; empty tags; endDate before startDate
        var bad = new Sample(20, "Bob", Status.DISABLED, List.of(),
                LocalDate.of(2025,1,10), LocalDate.of(2025,1,5),
                null, null, false, 0.0);

        var r = v.validate(bad);
        assertFalse(r.isValid());
        assertTrue(r.getErrors().stream().anyMatch(e -> e.contains("one of")), r.toString());
        assertTrue(r.getErrors().stream().anyMatch(e -> e.contains("must not be empty")), r.toString());
        assertTrue(r.getErrors().stream().anyMatch(e -> e.contains("on/after startDate")), r.toString());
    }

    @Test
    void objectAwareCustomFieldRule() {
        var v = Validator.<Sample>builder()
                .fieldRule("discount").optional()
                .customWithObject((s, val) -> s.vip || ((Number)val).doubleValue() <= 0.10,
                        "non-VIP discount must be ≤ 10%")
                .done()
                .build();

        var nonVipTooHigh = new Sample(20, "Bob", Status.ACTIVE, List.of("x"),
                LocalDate.now(), LocalDate.now(), null, null, false, 0.2);

        var r = v.validate(nonVipTooHigh);
        assertFalse(r.isValid());
        assertTrue(r.getErrors().stream().anyMatch(e -> e.contains("≤ 10%")), r.toString());
    }
}
