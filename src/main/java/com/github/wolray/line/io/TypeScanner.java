package com.github.wolray.line.io;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author wolray
 */
public class TypeScanner {
    private static boolean hasTypeFields = false;
    private static boolean hasParser = false;
    private static boolean hasFormatter = false;

    public static synchronized void scan(Class<?> clazz) {
        Map<Class<?>, Object> scanMap = ScanMap.INSTANCE;
        if (scanMap.containsKey(clazz)) {
            return;
        }
        scanMap.put(clazz, Object.class);

        for (Field f : clazz.getDeclaredFields()) {
            Fields fields = f.getAnnotation(Fields.class);
            Class<?> type = f.getType();
            if (type == DataMapper.class) {
                initStaticByTypePara(f, fields, DataMapper::new);
            } else if (fields != null) {
                addTypeFields(type, fields);
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

    static synchronized Fields getFields(Class<?> type) {
        Fields fields = type.getAnnotation(Fields.class);
        if (fields != null) {
            return fields;
        }
        fields = TempFieldsMap.INSTANCE.get(type);
        if (fields != null) {
            return fields;
        }
        if (hasTypeFields) {
            return TypeFieldsMap.INSTANCE.get(type);
        }
        return null;
    }

    private static void initStaticByTypePara(Field f, Fields fields,
        Function<Class<?>, ?> typeToObject) {
        if (Modifier.isStatic(f.getModifiers())) {
            Type genericType = f.getGenericType();
            if (genericType instanceof ParameterizedType) {
                Type argument = ((ParameterizedType)genericType).getActualTypeArguments()[0];
                Class<?> type = (Class<?>)argument;
                if (fields != null) {
                    TempFieldsMap.INSTANCE.put(type, fields);
                    setStaticField(f, () -> typeToObject.apply(type));
                    TempFieldsMap.INSTANCE.remove(type);
                } else {
                    setStaticField(f, () -> typeToObject.apply(type));
                }
            }
        }
    }

    private static void setStaticField(Field field, Supplier<?> supplier) {
        try {
            field.setAccessible(true);
            Object value = field.get(null);
            if (value == null) {
                field.set(null, supplier.get());
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addTypeFields(Class<?> type, Fields fields) {
        TypeFieldsMap.INSTANCE.put(type, fields);
        hasTypeFields = true;
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

    private static class TempFieldsMap {
        private static final Map<Class<?>, Fields> INSTANCE = new ConcurrentHashMap<>();
    }

    private static class ParserMap {
        private static final Map<Class<?>, Function<String, ?>> INSTANCE = new ConcurrentHashMap<>();
    }

    private static class FormatterMap {
        private static final Map<Class<?>, Function<Object, String>> INSTANCE = new ConcurrentHashMap<>();
    }
}
