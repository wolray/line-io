package com.github.wolray.line.io;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * @author ray
 */
class ValuesBase<T> {
    final Class<T> type;
    final FieldContext[] fieldContexts;

    ValuesBase(Class<T> type) {
        this.type = type;
        Fields fields = TypeScanner.getFields(type);
        Stream<FieldContext> stream = getFields(type, fields)
            .filter(f -> checkModifier(f.getModifiers()))
            .map(FieldContext::new);
        fieldContexts = filterFields(stream, fields).toArray(FieldContext[]::new);
    }

    static boolean checkModifier(int modifier) {
        return (modifier & (Modifier.STATIC | Modifier.FINAL | Modifier.TRANSIENT)) == 0;
    }

    private Stream<Field> getFields(Class<T> type, Fields fields) {
        if (fields != null && fields.pojo()) {
            return Arrays.stream(type.getDeclaredFields())
                .filter(f -> Modifier.isPrivate(f.getModifiers()))
                .peek(f -> f.setAccessible(true));
        } else {
            return Arrays.stream(type.getFields());
        }
    }

    void processStaticMethods() {
        for (Method m : type.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                Class<?>[] types = m.getParameterTypes();
                Class<?> returnType = m.getReturnType();
                if (types.length == 1 && returnType != void.class) {
                    processMethod(m, types[0], returnType);
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
            String[] omit = fields.omit();
            if (omit.length > 0) {
                Set<String> set = new HashSet<>(Arrays.asList(omit));
                return stream.filter(c -> !set.contains(c.field.getName()));
            }
            String regex = fields.regex();
            if (!regex.isEmpty()) {
                return stream.filter(c -> c.field.getName().matches(regex));
            }
        }
        return stream;
    }

    public static class FieldContext {
        public final Field field;
        UnaryOperator<String> mapper;
        Function<String, ?> parser;
        Function<Object, ?> function;
        Function<Object, String> printer;

        FieldContext(Field field) {
            this.field = field;
        }

        void composeMapper() {
            if (mapper != null) {
                parser = parser.compose(mapper);
            }
        }

        public Object parse(String s) {
            return !s.isEmpty() ? parser.apply(s) : null;
        }

        public Object convert(Object o) {
            return o != null ? function.apply(o) : null;
        }

        void set(Object t, Object o) {
            try {
                field.set(t, o);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        String print(Object o) {
            return o != null ? printer.apply(o) : "";
        }

        Object get(Object t) {
            try {
                return field.get(t);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
