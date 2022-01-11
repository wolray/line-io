package com.github.wolray.line.io;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import static com.github.wolray.line.io.TypeScanner.invoke;

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
        Function<Object, String> fmt = String::valueOf;
        Map<Class<?>, Function<Object, String>> map = TypeScanner.getFormatterMap();
        if (map == null) {
            map = Collections.emptyMap();
        }
        for (FieldContext context : fieldContexts) {
            context.formatter = map.getOrDefault(context.field.getType(), fmt);
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
