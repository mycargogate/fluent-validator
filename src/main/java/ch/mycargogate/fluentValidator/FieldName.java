package ch.mycargogate.fluentValidator;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

class FieldName {

    public static <T> String nameOf(GetterRef<T, ?> ref) {
        try {
            Method writeReplace = ref.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(ref);
            String methodName = lambda.getImplMethodName();
            if (methodName.startsWith("get")) {
                return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            } else if (methodName.startsWith("is")) {
                return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
            }
            throw new IllegalArgumentException("Not a getter: " + methodName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
