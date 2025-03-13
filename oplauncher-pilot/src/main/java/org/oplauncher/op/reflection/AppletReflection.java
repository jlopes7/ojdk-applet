package org.oplauncher.op.reflection;

import org.oplauncher.OPLauncherException;

import java.applet.Applet;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.oplauncher.ErrorCode.REFLECTION_ERROR;

public abstract class AppletReflection {

    protected <A extends Applet>Method getMethodFromNumOfParameters(final A applet, String methodName, final int numOfParameters) {
        /*
         * TODO: This implementation will fail with Polymorphism, when the same number of parameters can match
         *       different object types - polymorphism is bad practice anyways and it can have several problems
         *       when the parameters are null! ;)
         */
        Class<? extends Applet> klass = applet.getClass();

        // Retrieve all declared methods
        Method[] methods = klass.getMethods();

        if (methodName != null) {
            // Iterate over methods to find a match
            for (Method method : methods) {
                if (method.getName().equals(methodName) && method.getParameterCount() == numOfParameters) {
                    return method;
                }
            }
        }

        throw new OPLauncherException("Method " + methodName + " with " + numOfParameters + " parameters was not found.", REFLECTION_ERROR);
    }

    protected List<?> convertClientParametersToMethodParameters(List<String> parameters, Method method) {
        /*
         * TODO: This transformation process assumes that all method parameters are passed as Strings
         *       and only primitive types are supported. Custom object types will fail with an exception
         */
        int counter = 0;
        List<Object> formattedParameters = new ArrayList<>();
        List<Class<?>> paramTypes = Arrays.asList(method.getParameterTypes());

        // Quick parameter types verification
        boolean haveNonPrimitiveParameters = paramTypes.stream().allMatch(type -> type.isPrimitive() || type.equals(String.class));
        if (!haveNonPrimitiveParameters) {
            throw new OPLauncherException(String.format("The given method (%s) contains parameters that are not primitives", method.getName()), REFLECTION_ERROR);
        }
        if (parameters.size() != paramTypes.size()) {
            throw new OPLauncherException(String.format("The number of parameters expected by the method (%s) doesn't match the number of given parameters: %d != %d", method.getName(), paramTypes.size(), parameters.size()), REFLECTION_ERROR);
        }

        // Create the new list of parameters
        for (Class<?> paramType: paramTypes) {
            String paramName = paramType.getSimpleName();
            if (paramName.equalsIgnoreCase("int")) formattedParameters.add(Integer.valueOf(parameters.get(counter++)));
            else if (paramName.equalsIgnoreCase("long")) formattedParameters.add(Long.valueOf(parameters.get(counter++)));
            else if (paramName.equalsIgnoreCase("float")) formattedParameters.add(Float.valueOf(parameters.get(counter++)));
            else if (paramName.equalsIgnoreCase("double")) formattedParameters.add(Float.valueOf(parameters.get(counter++)));
            else if (paramName.equalsIgnoreCase("byte")) formattedParameters.add(Byte.valueOf(parameters.get(counter++)));
            else if (paramName.equalsIgnoreCase("char")) formattedParameters.add(new Character(parameters.get(counter++).charAt(0)));
            else if (paramType.equals(String.class)) formattedParameters.add(parameters.get(counter++));
            else {
                throw new OPLauncherException(String.format("Unsupported primitive type (Method: %s): %s", method.getName(), paramName), REFLECTION_ERROR);
            }
        }

        return formattedParameters;
    }

    protected List<Parameter> getMethodParameters(Method method) {
        return Arrays.asList(method.getParameters());
    }

}
