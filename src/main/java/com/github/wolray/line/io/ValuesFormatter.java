package com.github.wolray.line.io;

import java.lang.reflect.Method;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * @author ray
 */
public class ValuesFormatter<T> extends ValuesBase<T> implements Function<T, String> {
    private final String sep;

    public ValuesFormatter(Class<T> type, String sep) {
        super(type);
        this.sep = sep;
        initFormatters();
    }

    private void initFormatters() {
        Function<Object, String> function = String::valueOf;
        for (FieldContext context : fieldContexts) {
            context.formatter = function;
        }
        processStaticMethods();
    }

    @Override
    void processMethod(Method method, Class<?> paraType, Class<?> returnType) {
        if (paraType != String.class && returnType == String.class) {
            method.setAccessible(true);
            Function<Object, String> function = s -> (String)invoke(method, s);
            filterFields(method.getAnnotation(Fields.class))
                .filter(c -> c.field.getType() == paraType)
                .forEach(c -> c.formatter = function);
        }
    }

    String joinFields(Function<FieldContext, String> function) {
        StringJoiner joiner = new StringJoiner(sep);
        for (FieldContext context : fieldContexts) {
            joiner.add(function.apply(context));
        }
        return joiner.toString();
    }

    @Override
    public String apply(T t) {
        return joinFields(c -> c.format(t));
    }
}
