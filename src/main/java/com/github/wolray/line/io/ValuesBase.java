package com.github.wolray.line.io;

import java.lang.reflect.*;
import java.util.*;
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
        Fields fields = type.getAnnotation(Fields.class);
        Stream<FieldContext> stream = getFields(type, fields)
            .filter(f -> !isStatic(f) && !Modifier.isFinal(f.getModifiers()))
            .map(FieldContext::new);
        fieldContexts = filterFields(stream, fields).toArray(FieldContext[]::new);
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

    public static class FieldContext {
        public final Field field;
        UnaryOperator<String> mapper;
        Function<String, ?> parser;
        Function<Object, ?> function;
        Function<Object, String> formatter;

        FieldContext(Field field) {
            this.field = field;
        }

        void composeMapper() {
            if (mapper != null) {
                parser = parser.compose(mapper);
            }
        }

        public Object convertString(String s) {
            return parser.apply(s);
        }

        public Object convertObject(Object o) {
            return function.apply(o);
        }

        void set(Object t, Object o) {
            try {
                field.set(t, o);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        Object get(Object t) {
            try {
                return field.get(t);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        String format(Object t) {
            return formatter.apply(get(t));
        }
    }
}
