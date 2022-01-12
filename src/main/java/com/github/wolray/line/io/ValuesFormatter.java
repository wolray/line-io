package com.github.wolray.line.io;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import static com.github.wolray.line.io.TypeScanner.invoke;

/**
 * @author ray
 */
public class ValuesFormatter<T> extends ValuesBase<T> {
    public ValuesFormatter(Class<T> type) {
        super(type);
        initFormatters();
    }

    private void initFormatters() {
        Function<Object, String> toString = Object::toString;
        Map<Class<?>, Function<Object, String>> map = TypeScanner.getPrinterMap();
        for (FieldContext context : fieldContexts) {
            context.printer = map.getOrDefault(context.field.getType(), toString);
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
                .forEach(c -> c.printer = function);
        }
    }

    String join(String sep, Function<FieldContext, String> function) {
        StringJoiner joiner = new StringJoiner(sep);
        for (FieldContext context : fieldContexts) {
            joiner.add(function.apply(context));
        }
        return joiner.toString();
    }

    public Function<T, String> toPrinter(String sep) {
        return t -> join(sep, c -> c.print(c.get(t)));
    }
}
