package ch.mycargogate.fluentValidator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;

/**
 * Minimal CSV reader for validator rules with registry support.
 * CSV format:
 * field,<fieldName>,mandatory[,min=0][,max=100][,rule=email]
 * objectRule,<message>
 */
public class ValidatorReaderCSV<T> {

    private final CustomRegistry<T> registry;

    public ValidatorReaderCSV(CustomRegistry<T> registry) {
        this.registry = registry;
    }

    public Validator<T> fromResource(String resource) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream(resource),
                        "Resource not found: " + resource
                )
        ))) {
            Validator.Builder<T> builder = Validator.builder();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(",");
                if (parts[0].equalsIgnoreCase("field")) {
                    String fieldName = parts[1];
                    Validator.FieldRuleBuilder<T, Object> fr = builder.fieldRule(fieldName);
                    for (int i = 2; i < parts.length; i++) {
                        String token = parts[i].trim();
                        if (token.equals("mandatory")) fr.mandatory();
                        else if (token.equals("optional")) fr.optional();
                        else if (token.startsWith("min=")) fr.min(Double.parseDouble(token.substring(4)));
                        else if (token.startsWith("max=")) fr.max(Double.parseDouble(token.substring(4)));
                        else if (token.startsWith("rule=")) {
                            String ruleName = token.substring(5);
                            CustomRegistry.RuleEntry entry = registry.get(ruleName);
                            if (entry != null) {
                                fr.custom(entry.getPredicate(), entry.getMessage());
                            }
                        }
                    }
                    fr.done();
                } else if (parts[0].equalsIgnoreCase("objectRule")) {
                    builder.objectRule(o -> true, parts.length > 1 ? parts[1] : "Object rule failed");
                }
            }
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load validator from CSV", e);
        }
    }
}
