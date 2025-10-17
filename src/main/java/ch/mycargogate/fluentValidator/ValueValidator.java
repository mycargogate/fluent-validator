package ch.mycargogate.fluentValidator;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.List;

@Getter
@Setter
class ValueValidator<F> extends BaseValidator<F> {
    private Method getter;
    private boolean mandatory = false;

    public ValueValidator(String fieldName) {
        super(fieldName);
    }

    public ValueValidator(Class<F> clazz, String fieldName) {
        super(fieldName);

        // Check if the getter exists
        if (clazz != null)
            this.getter = getGetter(clazz);
    }

    public ValueValidator(Method getter) {
        super();

        // Find field name
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

        setFieldName(fieldName);
        this.getter = getter;
    }

    public Method getGetter(Class<?> clazz) {
        if (this.getter == null) {

            // Cache it
            synchronized (this) {

                for (var getIs : new String[]{"get", "is"}) {
                    var fieldNameUpper = getFieldNameFirstUpper();
                    String getterName = getIs + fieldNameUpper;

                    Class<?> theClass = clazz;
                    do {

                        try {
                            this.getter = theClass.getDeclaredMethod(getterName);
                        } catch (NoSuchMethodException e) {
                        }

                    } while ((theClass = theClass.getSuperclass()) != null);

                    if (this.getter != null) break;
                }

                if (getter == null)
                    throw new RuntimeException(String.format("Cannot find getter method for field %s in class %s", getFieldName(), clazz.getSimpleName()));
            }
        }

        return this.getter;
    }

    @Override
    protected void doValidate(String holder, F value, List<ValidationError> errors) {
        super.doValidate(holder, value, errors);

        if (value == null) {
            if (isMandatory()) {
                String message = ValidatorMessages.message(ErrorCode.MISSING_VALUE, getFullFieldName(holder));
                addErrorMessage(holder, errors, ErrorCode.MISSING_VALUE,message);
            }
        }
    }
}
