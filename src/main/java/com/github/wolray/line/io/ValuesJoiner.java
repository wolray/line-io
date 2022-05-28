package com.github.wolray.line.io;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.wolray.line.io.TypeValues.invoke;

/**
 * @author wolray
 */
public class ValuesJoiner<T> {
    private final TypeValues<T> typeValues;
    private final TypeValues.Attr[] attrs;

    public ValuesJoiner(TypeValues<T> typeValues) {
        this.typeValues = typeValues;
        attrs = typeValues.toAttrs();
        initFormatters();
    }

    private void initFormatters() {
        Function<Object, String> toString = Object::toString;
        for (TypeValues.Attr attr : attrs) {
            attr.formatter = toString;
        }
        TypeValues.processSimpleMethods(typeValues.type, this::processMethod);
    }

    void processMethod(TypeValues.SimpleMethod simpleMethod) {
        Method method = simpleMethod.method;
        Class<?> paraType = simpleMethod.paraType;
        if (paraType != String.class && simpleMethod.returnType == String.class) {
            method.setAccessible(true);
            Function<Object, String> function = s -> (String)invoke(method, s);
            Fields fields = method.getAnnotation(Fields.class);
            Predicate<Field> predicate = FieldSelector.toPredicate(fields);
            Arrays.stream(attrs)
                .filter(a -> predicate.test(a.field))
                .filter(a -> a.field.getType() == paraType)
                .forEach(a -> a.formatter = function);
        }
    }

    String join(String sep) {
        return join(sep, a -> a.field.getName());
    }

    String join(String sep, Function<TypeValues.Attr, String> function) {
        StringJoiner joiner = new StringJoiner(sep);
        for (TypeValues.Attr attr : attrs) {
            joiner.add(function.apply(attr));
        }
        return joiner.toString();
    }

    public Function<T, String> toFormatter(String sep) {
        return t -> join(sep, a -> a.format(a.get(t)));
    }
}
