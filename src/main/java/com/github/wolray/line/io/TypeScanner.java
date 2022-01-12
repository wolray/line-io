package com.github.wolray.line.io;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author wolray
 */
public class TypeScanner {
    private static Map<Class<?>, Fields> typeFieldsMap;
    private static Map<Class<?>, Function<String, ?>> parserMap;
    private static Map<Class<?>, Function<Object, String>> formatterMap;

    public static synchronized void scan(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            Fields annotation = field.getAnnotation(Fields.class);
            if (annotation != null) {
                if (typeFieldsMap == null) {
                    typeFieldsMap = new ConcurrentHashMap<>();
                }
                typeFieldsMap.put(field.getType(), annotation);
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == void.class) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                continue;
            }
            Class<?> parameterType = parameterTypes[0];
            if (parameterType == returnType) {
                continue;
            }

            if (parameterType == String.class) {
                if (parserMap == null) {
                    parserMap = new ConcurrentHashMap<>();
                }
                method.setAccessible(true);
                parserMap.put(returnType, s -> invoke(method, s));
            } else if (returnType == String.class) {
                if (formatterMap == null) {
                    formatterMap = new ConcurrentHashMap<>();
                }
                method.setAccessible(true);
                formatterMap.put(parameterType, o -> (String)invoke(method, o));
            }
        }
    }

    static Fields get(Class<?> type) {
        Fields fields = type.getAnnotation(Fields.class);
        if (fields != null) {
            return fields;
        }
        if (typeFieldsMap != null) {
            return typeFieldsMap.get(type);
        }
        return null;
    }

    static Map<Class<?>, Function<String, ?>> getParserMap() {
        return parserMap != null ? parserMap : Collections.emptyMap();
    }

    static Map<Class<?>, Function<Object, String>> getFormatterMap() {
        return formatterMap != null ? formatterMap : Collections.emptyMap();
    }

    static Object invoke(Method method, Object o) {
        try {
            return method.invoke(null, o);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }
}
