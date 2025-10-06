````
  __  __        ____                       ____       _       
|  \/  |_   _ / ___|__ _ _ __ __ _  ___  / ___| __ _| |_ ___
| |\/| | | | | |   / _` | '__/ _` |/ _ \| |  _ / _` | __/ _ \
| |  | | |_| | |__| (_| | | | (_| | (_) | |_| | (_| | ||  __/
|_|  |_|\__, |\____\__,_|_|  \__, |\___/ \____|\__,_|\__\___|
|___/                |___/
````
# MyCargoGate Fluent Validator

**MyCargoGate Fluent Validator** is a lightweight, extensible validation framework for Java objects.  
It provides a **fluent API** to build validation rules, supports **CSV/JSON configuration loaders**, and allows **custom reusable validators** via a **registry**.

---

## ✨ Features

- **Fluent API** for declaring validation rules
- Built-in constraints:
    - Mandatory / Optional fields
    - String checks (`notBlank`, regex, length)
    - Numeric ranges (`min`, `max`)
    - Enum membership
    - Collection size
    - Date ranges (`notBefore`, `notAfter`)
- **Object rules** for multi-field validation
- **Custom rules**:
    - Inline lambdas
    - Reusable via registry (`DefaultRegistry` or custom)
- **CSV and JSON loaders** to configure validation externally
- Structured validation errors:
    - Field name
    - Rule name
    - Message
- JUnit 5 tests included

---

## 🚀 Usage Example – Fluent API

```java
import ch.mycargogate.fluentValidator.ValidationError;
import ch.mycargogate.fluentValidator.ValidationResult;
import ch.mycargogate.fluentValidator.Validator;

public class Demo {
  static class User {
    String name;
    Integer age;
  }

  public static void main(String[] args) {
    Validator<User> validator = Validator.<User>validatorBuilder()
            .fieldRule("name").mandatory().notBlank().done()
            .fieldRule("age").mandatory().min(0).max(120).done()
            .objectRule(u -> u.age != null && u.age >= 18, "User must be an adult")
            .build();

    User user = new User();
    user.name = "Alice";
    user.age = 15;

    ValidationResult result = validator.validate(user);

    if (!result.isValid()) {
      for (ValidationError e : result.getErrors()) {
        System.out.printf("Field=%s | Rule=%s | Msg=%s%n",
                e.getField(), e.getRule(), e.getMessage());
      }
    }
  }
}
```

Output:
```
Field=null | Rule=objectRule | Msg=User must be an adult
```

---

## 📂 Load Validators from CSV

**File: `src/main/resources/validator.csv`**

```
field,email,mandatory,rule=email
field,age,mandatory,rule=positive
objectRule,User must be an adult
```

**Java code:**
```java
DefaultRegistry<Object> registry = new DefaultRegistry<>();
Validator<User> validator = new ValidatorReaderCSV<User>(registry)
    .fromResource("validator.csv");
```

---

## 📂 Load Validators from JSON

**File: `src/main/resources/validator.json`**

```json
{
  "fields": [
    { "name": "email", "mandatory": true, "rule": "email" },
    { "name": "age", "mandatory": true, "rule": "positive" }
  ],
  "objectRules": [
    { "message": "User must be an adult" }
  ]
}
```

**Java code:**
```java
DefaultRegistry<Object> registry = new DefaultRegistry<>();
Validator<User> validator = new ValidatorReaderJson<User>(registry)
    .fromResource("validator.json");
```

---

## 🛠️ Custom Validators with Registry

You can define reusable validators in the `DefaultRegistry` or extend your own `CustomRegistry`.

**Register a new rule:**
```java
DefaultRegistry<Object> registry = new DefaultRegistry<>();
registry.register("even", v -> v instanceof Integer i && i % 2 == 0,
    "Must be an even number");
```

**Use in Fluent API:**
```java
Validator<User> validator = Validator.<User>validatorBuilder()
    .fieldRule("age")
        .custom(registry.get("even").getPredicate(),
                registry.get("even").getMessage())
        .done()
    .build();
```

**Use in CSV:**
```
field,age,mandatory,rule=even
```

**Use in JSON:**
```json
{
  "fields": [
    { "name": "age", "mandatory": true, "rule": "even" }
  ]
}
```

---

## 🧪 Running Tests

```bash
mvn clean test
```

Covers:
- Core validation API
- CSV loader
- JSON loader
- Registry-based rules
- Custom dynamic rules
- Object rules

---

## 📦 Run Demo

```bash
mvn clean compile exec:java -Dexec.mainClass="ch.mycargogate.fluentValidator.App"
```

---

## 📜 License

MIT License. Free to use and extend.
