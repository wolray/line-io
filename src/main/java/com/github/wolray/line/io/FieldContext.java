package com.github.wolray.line.io;

import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * @author ray
 */
public class FieldContext {
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
