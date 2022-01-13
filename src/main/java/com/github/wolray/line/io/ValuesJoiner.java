package com.github.wolray.line.io;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.wolray.line.io.TypeData.invoke;

/**
 * @author ray
 */
public class ValuesJoiner<T> {
    private final TypeData<T> typeData;
    private final TypeData.Attr[] attrs;

    public ValuesJoiner(TypeData<T> typeData) {
        this.typeData = typeData;
        attrs = typeData.toAttrs();
        initFormatters();
    }

    private void initFormatters() {
        Function<Object, String> toString = Object::toString;
        for (TypeData.Attr attr : attrs) {
            attr.formatter = toString;
        }
        TypeData.processSimpleMethods(typeData.type, this::processMethod);
    }

    void processMethod(TypeData.SimpleMethod simpleMethod) {
        Method method = simpleMethod.method;
        Class<?> paraType = simpleMethod.paraType;
        if (paraType != String.class && simpleMethod.returnType == String.class) {
            method.setAccessible(true);
            Function<Object, String> function = s -> (String)invoke(method, s);
            Fields fields = method.getAnnotation(Fields.class);
            Predicate<Field> predicate = TypeData.makePredicate(fields);
            Arrays.stream(attrs)
                .filter(a -> predicate.test(a.field))
                .filter(a -> a.field.getType() == paraType)
                .forEach(c -> c.formatter = function);
        }
    }

    String join(String sep, Function<TypeData.Attr, String> function) {
        StringJoiner joiner = new StringJoiner(sep);
        for (TypeData.Attr attr : attrs) {
            joiner.add(function.apply(attr));
        }
        return joiner.toString();
    }

    public Function<T, String> toFormatter(String sep) {
        return t -> join(sep, c -> c.format(c.get(t)));
    }
}
