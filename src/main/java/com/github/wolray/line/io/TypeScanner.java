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
    private static boolean hasTypeFields = false;
    private static boolean hasParser = false;
    private static boolean hasFormatter = false;

    public static void scan(Class<?> clazz) {
        Map<Class<?>, Object> scanMap = ScanMap.INSTANCE;
        if (scanMap.containsKey(clazz)) {
            return;
        }
        scanMap.put(clazz, Object.class);

        for (Field f : clazz.getDeclaredFields()) {
            Class<?> type = f.getType();
            Fields fields = f.getAnnotation(Fields.class);
            if (fields != null) {
                TypeFieldsMap.INSTANCE.put(type, fields);
                hasTypeFields = true;
            }
            getDataMapper(type, f.getAnnotation(WithDataMapper.class));
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
                method.setAccessible(true);
                ParserMap.INSTANCE.put(returnType, s -> invoke(method, s));
                hasParser = true;
            } else if (returnType == String.class) {
                method.setAccessible(true);
                FormatterMap.INSTANCE.put(parameterType, o -> (String)invoke(method, o));
                hasFormatter = true;
            }
        }
    }

    public static <T> DataMapper<T> getDataMapper(Class<T> type) {
        return getDataMapper(type, type.getAnnotation(WithDataMapper.class));
    }

    @SuppressWarnings("unchecked")
    private static <T> DataMapper<T> getDataMapper(Class<T> type, WithDataMapper withDataMapper) {
        if (withDataMapper != null) {
            return (DataMapper<T>)DataMapperMap.INSTANCE.computeIfAbsent(type, t -> new DataMapper<>(t, withDataMapper.sep()));
        }
        return null;
    }

    static Fields getFields(Class<?> type) {
        Fields fields = type.getAnnotation(Fields.class);
        if (fields != null) {
            return fields;
        }
        if (hasTypeFields) {
            return TypeFieldsMap.INSTANCE.get(type);
        }
        return null;
    }

    static Map<Class<?>, Function<String, ?>> getParserMap() {
        return hasParser ? ParserMap.INSTANCE : Collections.emptyMap();
    }

    static Map<Class<?>, Function<Object, String>> getFormatterMap() {
        return hasFormatter ? FormatterMap.INSTANCE : Collections.emptyMap();
    }

    static Object invoke(Method method, Object o) {
        try {
            return method.invoke(null, o);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private static class ScanMap {
        private static final Map<Class<?>, Object> INSTANCE = new ConcurrentHashMap<>();
    }

    private static class TypeFieldsMap {
        private static final Map<Class<?>, Fields> INSTANCE = new ConcurrentHashMap<>();
    }

    private static class DataMapperMap {
        private static final Map<Class<?>, DataMapper<?>> INSTANCE = new ConcurrentHashMap<>();
    }

    private static class ParserMap {
        private static final Map<Class<?>, Function<String, ?>> INSTANCE = new ConcurrentHashMap<>();
    }

    private static class FormatterMap {
        private static final Map<Class<?>, Function<Object, String>> INSTANCE = new ConcurrentHashMap<>();
    }
}
