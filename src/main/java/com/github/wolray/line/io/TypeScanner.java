package com.github.wolray.line.io;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author wolray
 */
public class TypeScanner {
    private static Map<Class<?>, Object> scanMap;
    private static Map<Class<?>, Fields> typeFieldsMap;
    private static Map<Class<?>, Function<String, ?>> parserMap;
    private static Map<Class<?>, Function<Object, String>> formatterMap;

    public static synchronized void scan(Class<?> clazz) {
        if (scanMap == null) {
            scanMap = new ConcurrentHashMap<>();
        }
        if (scanMap.containsKey(clazz)) {
            return;
        }
        scanMap.put(clazz, Object.class);
        for (Method method : clazz.getDeclaredMethods()) {
            Fields fields = method.getAnnotation(Fields.class);
            Class<?> returnType = method.getReturnType();
            if (returnType != void.class) {
                if (fields != null) {
                    if (typeFieldsMap == null) {
                        typeFieldsMap = new ConcurrentHashMap<>();
                    }
                    typeFieldsMap.put(returnType, fields);
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 1) {
                    Class<?> parameterType = parameterTypes[0];
                    if (parameterType == String.class) {
                        if (parserMap == null) {
                            parserMap = new ConcurrentHashMap<>();
                        }
                        parserMap.put(returnType, s -> invoke(method, s));
                    } else if (returnType == String.class) {
                        if (formatterMap == null) {
                            formatterMap = new ConcurrentHashMap<>();
                        }
                        formatterMap.put(parameterType, o -> (String)invoke(method, o));
                    }
                }
            }
        }
    }

    public static synchronized void clear() {
        scanMap.clear();
        typeFieldsMap = null;
    }

    public static Fields get(Class<?> type) {
        Fields fields = type.getAnnotation(Fields.class);
        if (fields != null) {
            return fields;
        }
        if (typeFieldsMap != null) {
            return typeFieldsMap.get(type);
        }
        return null;
    }

    public static Map<Class<?>, Function<String, ?>> getParserMap() {
        return parserMap != null ? parserMap : Collections.emptyMap();
    }

    public static Map<Class<?>, Function<Object, String>> getFormatterMap() {
        return formatterMap != null ? formatterMap : Collections.emptyMap();
    }

    public static Object invoke(Method method, Object o) {
        try {
            return method.invoke(null, o);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }
}
