package com.github.wolray.line.io;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * @author wolray
 */
public class TypeValues<T> {
    public final Class<T> type;
    public final Field[] values;

    public TypeValues(Class<T> type) {
        this(type, FieldSelector.of(type.getAnnotation(Fields.class)));
    }

    public TypeValues(Class<T> type, FieldSelector selector) {
        this.type = type;
        Stream<Field> stream = getFields(type, selector)
            .filter(f -> checkModifier(f.getModifiers()));
        Predicate<Field> predicate = selector.toPredicate();
        values = stream.filter(predicate).toArray(Field[]::new);
    }

    private static boolean checkModifier(int modifier) {
        return (modifier & (Modifier.STATIC | Modifier.FINAL | Modifier.TRANSIENT)) == 0;
    }

    static Object invoke(Method method, Object o) {
        try {
            return method.invoke(null, o);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    static void processSimpleMethods(Class<?> type, Consumer<SimpleMethod> consumer) {
        for (Method m : type.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                Class<?>[] parameterTypes = m.getParameterTypes();
                Class<?> returnType = m.getReturnType();
                if (parameterTypes.length == 1 && returnType != void.class) {
                    consumer.accept(new SimpleMethod(m, parameterTypes[0], returnType));
                }
            }
        }
    }

    Attr[] toAttrs() {
        return Arrays.stream(values).map(Attr::new).toArray(Attr[]::new);
    }

    private Stream<Field> getFields(Class<T> type, FieldSelector selector) {
        if (selector != null && selector.getPojo()) {
            return Arrays.stream(type.getDeclaredFields())
                .filter(f -> Modifier.isPrivate(f.getModifiers()))
                .peek(f -> f.setAccessible(true));
        } else {
            return Arrays.stream(type.getFields());
        }
    }

    static class SimpleMethod {
        final Method method;
        final Class<?> paraType;
        final Class<?> returnType;

        SimpleMethod(Method method, Class<?> paraType, Class<?> returnType) {
            this.method = method;
            this.paraType = paraType;
            this.returnType = returnType;
        }
    }

    public static class Attr {
        public final Field field;
        UnaryOperator<String> mapper;
        Function<String, ?> parser;
        Function<Object, ?> function;
        Function<Object, String> formatter;

        Attr(Field field) {
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

        public String format(Object o) {
            return o != null ? formatter.apply(o) : "";
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
    }
}
