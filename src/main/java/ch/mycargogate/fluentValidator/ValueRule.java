package ch.mycargogate.fluentValidator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

class ValueRule<F> extends ValidationRule {
    public static record CustomRule(Predicate<Object> predicate, String message) {}

    private final String fieldName;
    private Method getter;
    private boolean mandatory = false;
    private List<FieldRule.CustomRule> customRules = new ArrayList<>();

    public ValueRule(String fieldName) {
        this.fieldName = fieldName;
    }

    public ValueRule(Class<F> clazz, String fieldName) {
        this.fieldName = fieldName;

        // Check if the getter exists
        if(clazz != null)
            this.getter = getGetter(clazz);
    }

    public ValueRule(Method getter) {
        var getterName = getter.getName();
        String fieldName;

        if (getterName.startsWith("get")) {
            fieldName = getterName.substring(3);
            fieldName = Character.toLowerCase(fieldName.charAt(0)) + getterName.substring(1);
        } else if (getterName.startsWith("is")) {
            fieldName = getterName.substring(2);
            fieldName = Character.toLowerCase(fieldName.charAt(0)) + getterName.substring(1);
        } else
            throw new RuntimeException("Not a getter: " + getterName);

        this.fieldName = fieldName;
        this.getter = getter;
    }

    public Method getGetter(Class<F> clazz) {
        if (this.getter == null) {

            // Cache it
            synchronized (this) {

                for(var getIs : new String[] {"get", "is"}) {
                    String fieldNameUpper = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                    String getterName = getIs + fieldNameUpper;

                    Class<?> theClass = clazz;
                    do {

                        try {
                            this.getter = theClass.getDeclaredMethod(getterName);
                        } catch (NoSuchMethodException e) {
                        }

                    } while ((theClass = theClass.getSuperclass()) != null);

                    if(this.getter != null) break;
                }

                if (getter == null)
                    throw new RuntimeException(String.format("Cannot find getter method for field %s in class %s", fieldName, clazz.getSimpleName()));
            }
        }

        return this.getter;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public List<CustomRule> getCustomRules() {
        return customRules;
    }

    public void setCustomRules(List<CustomRule> customRules) {
        this.customRules = customRules;
    }

    void validate(String holder, Object object, Collection<ValidationError> errors) {
        try {
            @SuppressWarnings("unchecked")
            F value = (F)getGetter((Class<F>)object.getClass()).invoke(object);

            doValidate(holder, value, errors);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void doValidate(String holder, F value, Collection<ValidationError> errors) {

        if (value == null) {
            if (isMandatory()) {
                errors.add(new ValidationError(holder, fieldName, "mandatory", "is mandatory"));
            }
            return;
        }

        // custom rules
        for(var customRule : customRules) {
            if (!customRule.predicate().test(value)) {
                errors.add(new ValidationError(holder, fieldName, "custom", customRule.message()));
            }
        }
    }

    String getHolderPrefix(String holder) {
        if (holder == null)
            return  "";
        return holder + ".";
    }
}
