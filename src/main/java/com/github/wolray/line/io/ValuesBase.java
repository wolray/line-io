package com.github.wolray.line.io;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author ray
 */
class ValuesBase<T> {
    final Class<T> type;
    final FieldContext[] fieldContexts;

    ValuesBase(Class<T> type) {
        this.type = type;
        Stream<FieldContext> stream = Arrays.stream(type.getFields())
            .filter(f -> !isStatic(f) && !Modifier.isFinal(f.getModifiers()))
            .map(FieldContext::new);
        fieldContexts = filterFields(stream, type.getAnnotation(Fields.class))
            .toArray(FieldContext[]::new);
    }

    static boolean isStatic(Member m) {
        return Modifier.isStatic(m.getModifiers());
    }

    static Object invoke(Method method, Object o) {
        try {
            return method.invoke(null, o);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    void processStaticMethods() {
        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> c = type; c != Object.class; c = c.getSuperclass()) {
            classes.add(c);
        }
        Collections.reverse(classes);
        for (Class<?> c : classes) {
            for (Method m : c.getDeclaredMethods()) {
                if (isStatic(m)) {
                    Class<?>[] types = m.getParameterTypes();
                    Class<?> returnType = m.getReturnType();
                    if (types.length == 1 && returnType != void.class) {
                        processMethod(m, types[0], returnType);
                    }
                }
            }
        }
    }

    void processMethod(Method method, Class<?> paraType, Class<?> returnType) {}

    Stream<FieldContext> filterFields(Fields fields) {
        return filterFields(Arrays.stream(fieldContexts), fields);
    }

    Stream<FieldContext> filterFields(Stream<FieldContext> stream, Fields fields) {
        if (fields != null) {
            String[] use = fields.use();
            if (use.length > 0) {
                Set<String> set = new HashSet<>(Arrays.asList(use));
                return stream.filter(c -> set.contains(c.field.getName()));
            }
            String[] ignore = fields.ignore();
            if (ignore.length > 0) {
                Set<String> set = new HashSet<>(Arrays.asList(ignore));
                return stream.filter(c -> !set.contains(c.field.getName()));
            }
            String regex = fields.regex();
            if (!regex.isEmpty()) {
                return stream.filter(c -> c.field.getName().matches(regex));
            }
        }
        return stream;
    }
}
