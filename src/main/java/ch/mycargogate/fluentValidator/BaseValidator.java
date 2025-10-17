package ch.mycargogate.fluentValidator;

import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseValidator<F> {
    private String fieldName;

    private final List<RuleRunner<F>> rules = new ArrayList<>();

    public BaseValidator(String fieldName) {
        this.fieldName = fieldName;
    }

    public void addRule(RuleRunner<F> rule) {
        rules.add(rule);
    }

    public void addPredicate(Predicate<F> predicate, String code) {
        var rule = new RuleRunner<F>() {

            @Override
            public List<ErrorCodeMessage> run(String holder, F value) {
                if( ! predicate.test(value) ) {
                    String message = String.format(ValidatorMessages.message(code, getFullFieldName(holder)));
                    return Collections.singletonList(new ErrorCodeMessage(code, message));
                }

                return null;
            }
        };

        addRule(rule);
    }
    private void addCustomRule(RuleRunner<F> runner) {
        rules.add(runner);
    }

    protected String getFullFieldName(String holder) {
        return holder == null? fieldName == null? "object": fieldName: fieldName == null? holder: holder + "." + fieldName;
    }

    protected String getFieldNameFirstUpper() {
        if(fieldName == null) return null;
        return  Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    protected void doValidate(String holder, F value, List<ValidationError> errors) {
        if(value != null)
            getRules().forEach(c -> addErrorMessages(holder, errors, c.run(holder, value)));
    }

    protected void addErrorMessage(String holder, List<ValidationError> errors, String code, String message) {
        if(message != null) {
            var error = new ValidationError(holder, fieldName, new ErrorCodeMessage(code, message));
            errors.add(error);
        }
    }

    protected void addErrorMessages(String holder, List<ValidationError> errors, List<ErrorCodeMessage> messages) {
        if(messages != null) {
            messages.forEach(m -> addErrorMessage(holder, errors, m.code(), m.message()));
        }
    }

    String getHolderPrefix(String holder) {
        if (holder == null)
            return "";
        return holder + ".";
    }
}
