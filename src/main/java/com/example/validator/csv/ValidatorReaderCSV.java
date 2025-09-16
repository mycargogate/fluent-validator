
package com.example.validator.csv;

import com.example.validator.Validator;

import java.io.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public final class ValidatorReaderCSV<T> {

    public Validator<T> load(InputStream csv, CustomRegistry<T> registry) throws IOException {
        Objects.requireNonNull(csv, "csv");
        Objects.requireNonNull(registry, "registry");

        var builder = Validator.<T>builder();
        Map<String, Validator.FieldOptionalBuilder<T>> openFieldBuilders = new LinkedHashMap<>();
        Map<String, Class<? extends Enum<?>>> enumTypePerField = new HashMap<>();

        try (var reader = new BufferedReader(new InputStreamReader(csv))) {
            String line;
            boolean firstNonCommentSeen = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (!firstNonCommentSeen) {
                    firstNonCommentSeen = true;
                    if (looksLikeHeader(line)) continue;
                }

                List<String> cols = parseCsvLine(line);
                while (cols.size() < 5) cols.add("");

                String type = cols.get(0).trim();
                String field = cols.get(1).trim();
                String rule  = cols.get(2).trim();
                String arg1  = cols.get(3).trim();
                String arg2  = cols.get(4).trim();

                if ("field".equalsIgnoreCase(type)) {
                    if (field.isEmpty()) throw new IllegalArgumentException("Field row missing field name: " + line);

                    var fbuilder = openFieldBuilders.get(field);
                    if (fbuilder == null) {
                        if ("mandatory".equalsIgnoreCase(rule)) {
                            builder.fieldRule(field).mandatory();
                            fbuilder = builder.fieldRule(field).optional();
                        } else if ("optional".equalsIgnoreCase(rule)) {
                            fbuilder = builder.fieldRule(field).optional();
                        } else {
                            fbuilder = builder.fieldRule(field).optional();
                        }
                        openFieldBuilders.put(field, fbuilder);
                    }

                    switch (rule) {
                        case "mandatory" -> {}
                        case "optional" -> {}
                        case "min" -> fbuilder.min(parseNumber(arg1));
                        case "max" -> fbuilder.max(parseNumber(arg1));
                        case "minLength" -> fbuilder.minLength(Integer.parseInt(arg1));
                        case "maxLength" -> fbuilder.maxLength(Integer.parseInt(arg1));
                        case "notBlank" -> fbuilder.notBlank();
                        case "regex" -> fbuilder.regex(arg1);
                        case "enumOf" -> {
                            Class<? extends Enum<?>> c = loadEnumType(arg1);
                            enumTypePerField.put(field, c);
                            @SuppressWarnings({"rawtypes","unchecked"})
                            Class raw = c;
                            fbuilder.enumOf(raw);
                        }
                        case "inEnum" -> {
                            Class<? extends Enum<?>> et = enumTypePerField.get(field);
                            if (et == null)
                                throw new IllegalArgumentException("inEnum requires a prior enumOf for field '" + field + "'");
                            Set<String> names = new LinkedHashSet<>(Arrays.asList(arg1.split("\\|")));
                            fbuilder.custom(v -> {
                                if (!(v instanceof Enum<?> e)) return false;
                                return et.isInstance(e) && names.contains(e.name());
                            }, "Field '" + field + "' must be one of " + names);
                        }
                        case "notEmpty" -> fbuilder.notEmpty();
                        case "minSize" -> fbuilder.minSize(Integer.parseInt(arg1));
                        case "maxSize" -> fbuilder.maxSize(Integer.parseInt(arg1));
                        case "notBeforeLD" -> fbuilder.notBefore(LocalDate.parse(arg1));
                        case "notAfterLD"  -> fbuilder.notAfter(LocalDate.parse(arg1));
                        case "notBeforeLDT"-> fbuilder.notBefore(LocalDateTime.parse(arg1));
                        case "notAfterLDT" -> fbuilder.notAfter(LocalDateTime.parse(arg1));
                        case "notBeforeInstant" -> fbuilder.notBefore(Instant.parse(arg1));
                        case "notAfterInstant"  -> fbuilder.notAfter(Instant.parse(arg1));
                        case "customValue" -> {
                            var p = registry.valuePredicate(arg1);
                            if (p == null) throw new IllegalArgumentException("Unknown customValue predicate: " + arg1);
                            fbuilder.custom(p, unquoteIfNeeded(arg2));
                        }
                        case "customWithObject" -> {
                            var p = registry.objectAndValuePredicate(arg1);
                            if (p == null) throw new IllegalArgumentException("Unknown customWithObject predicate: " + arg1);
                            fbuilder.customWithObject(p, unquoteIfNeeded(arg2));
                        }
                        default -> {
                            if (!Set.of("mandatory","optional").contains(rule))
                                throw new IllegalArgumentException("Unknown field rule: " + rule);
                        }
                    }

                } else if ("object".equalsIgnoreCase(type)) {
                    switch (rule) {
                        case "objectCustom", "objectRule" -> {
                            var p = registry.objectPredicate(arg1);
                            if (p == null) throw new IllegalArgumentException("Unknown object predicate: " + arg1);
                            String msg = unquoteIfNeeded(arg2);
                            builder = builder.objectRuleChain(p, msg);
                        }
                        default -> throw new IllegalArgumentException("Unknown object rule: " + rule);
                    }
                } else {
                    throw new IllegalArgumentException("Unknown type (must be field|object): " + type);
                }
            }
        }

        for (var fb : openFieldBuilders.values()) {
            fb.done();
        }
        return builder.build();
    }

    private static boolean looksLikeHeader(String line) {
        String l = line.toLowerCase(Locale.ROOT);
        return l.contains("type") && l.contains("rule");
    }

    private static String unquoteIfNeeded(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1);
        }
        return t;
    }

    private static Number parseNumber(String s) {
        try {
            if (s == null || s.isEmpty()) throw new NumberFormatException("empty");
            if (s.contains(".") || s.contains("e") || s.contains("E")) return Double.parseDouble(s);
            long l = Long.parseLong(s);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
            return l;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value: " + s, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum<?>> loadEnumType(String fqcn) {
        try {
            return (Class<? extends Enum<?>>) Class.forName(fqcn);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Enum type not found: " + fqcn, e);
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"'); i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == ',') {
                    out.add(sb.toString().trim());
                    sb.setLength(0);
                } else if (c == '"') {
                    inQuotes = true;
                } else {
                    sb.append(c);
                }
            }
        }
        out.add(sb.toString().trim());
        return out;
    }
}
