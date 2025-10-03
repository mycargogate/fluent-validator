package ch.mycargogate.fluentValidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.attribute.UserPrincipal;
import java.util.Iterator;
import java.util.Objects;

/**
 * Minimal JSON reader for validator rules.
 * JSON format:
 * {
 *   "fields": [
 *     {"name": "age", "mandatory": true, "min": 0, "max": 100}
 *   ],
 *   "objectRules": [
 *     {"message": "custom object validation failed"}
 *   ]
 * }
 */
public class ValidatorReaderJson {

    public Validator<?> fromResource(String resource) {
        try {
            InputStream is = Objects.requireNonNull(
                    getClass().getClassLoader().getResourceAsStream(resource),
                    "Resource not found: " + resource
            );
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            Validator.Builder<Object> builder = Validator.builder();

            var schemaNode = root.get("schema");
            if(schemaNode == null) throw new RuntimeException("schema field is missing");

            var className = schemaNode.asText();
            Class<Object> schema = null;
            try {
                //noinspection unchecked
                schema = (Class<Object>)Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("schema: class cannot be find for: " + className, e);
            }

            for (JsonNode f : root.get("fields")) {
                Validator.FieldRuleBuilder<Object, ?> fr = builder.fieldRule(schema, f.get("name").asText());
                if (f.has("mandatory") && f.get("mandatory").asBoolean()) fr.mandatory();
                if (f.has("notBlank") && f.get("notBlank").asBoolean()) fr.notBlank();
                if (f.has("min")) fr.min(f.get("min").asDouble());
                if (f.has("max")) fr.max(f.get("max").asDouble());
                if (f.has("minLength")) fr.minLength(f.get("minLength").asInt());
                if (f.has("maxLength")) fr.maxLength(f.get("maxLength").asInt());
                if (f.has("regex")) fr.regex(f.get("regex").asText());
                if (f.has("enum")) {
                    Iterator<JsonNode> it = f.get("enum").elements();
                    while (it.hasNext()) {
                        // just simple example: allow string enums
                        fr.inEnum(it.next().asText());
                    }
                }
                fr.done();
            }

            if (root.has("objectRules")) {
                for (JsonNode r : root.get("objectRules")) {
                    String msg = r.has("message") ? r.get("message").asText() : "Object rule failed";
                    builder.objectRule(o -> true, msg);
                }
            }
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load validator from JSON", e);
        }
    }
}
