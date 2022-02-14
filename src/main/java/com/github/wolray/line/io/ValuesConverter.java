package com.github.wolray.line.io;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.*;
import java.util.stream.Stream;

import static com.github.wolray.line.io.TypeValues.invoke;

/**
 * @author wolray
 */
public class ValuesConverter<V, T> implements Function<V, T> {
    protected final TypeValues<T> typeValues;
    protected final TypeValues.Attr[] attrs;
    private final ToIntFunction<V> sizeGetter;
    private final Constructor<T> constructor;
    private BiConsumer<T, V> filler;

    public ValuesConverter(TypeValues<T> typeValues, ToIntFunction<V> sizeGetter) {
        this.typeValues = typeValues;
        this.sizeGetter = sizeGetter;
        attrs = typeValues.toAttrs();
        constructor = initConstructor(typeValues.type);
        initParsers();
        filler = fillAll();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> T parseEnum(Class<?> type, String s) {
        try {
            return Enum.valueOf((Class<T>)type, s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void resetOrder(int[] slots) {
        filler = fillBySlots(slots);
    }

    private Constructor<T> initConstructor(Class<T> type) {
        try {
            return type.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void initParsers() {
        Map<Class<?>, Function<String, ?>> parserMap = new HashMap<>(11);
        parserMap.put(String.class, s -> s);
        parserMap.put(char.class, s -> s.charAt(0));
        parserMap.put(Character.class, s -> s.charAt(0));
        parserMap.put(boolean.class, Boolean::parseBoolean);
        parserMap.put(Boolean.class, Boolean::parseBoolean);
        parserMap.put(int.class, Integer::parseInt);
        parserMap.put(Integer.class, Integer::parseInt);
        parserMap.put(long.class, Long::parseLong);
        parserMap.put(Long.class, Long::parseLong);
        parserMap.put(double.class, Double::parseDouble);
        parserMap.put(Double.class, Double::parseDouble);

        for (TypeValues.Attr attr : attrs) {
            Class<?> type = attr.field.getType();
            if (type.isEnum()) {
                attr.parser = s -> parseEnum(type, s);
            } else {
                attr.parser = parserMap.get(type);
            }
        }
        TypeValues.processSimpleMethods(typeValues.type, this::processMethod);
        checkParsers();
    }

    void processMethod(TypeValues.SimpleMethod simpleMethod) {
        Method method = simpleMethod.method;
        Class<?> returnType = simpleMethod.returnType;
        if (simpleMethod.paraType == String.class) {
            Fields fields = method.getAnnotation(Fields.class);
            Predicate<Field> predicate = FieldSelector.toPredicate(fields);
            Stream<TypeValues.Attr> stream = Arrays.stream(attrs)
                .filter(a -> predicate.test(a.field));
            method.setAccessible(true);
            if (returnType == String.class) {
                UnaryOperator<String> mapper = s -> (String)invoke(method, s);
                stream.forEach(a -> a.mapper = mapper);
            } else {
                Function<String, Object> parser = s -> invoke(method, s);
                stream
                    .filter(a -> a.field.getType() == returnType)
                    .forEach(a -> a.parser = parser);
            }
        }
    }

    private void checkParsers() {
        for (TypeValues.Attr attr : attrs) {
            if (attr.parser == null) {
                String fmt = "cannot parse %s, please add a static method (String -> %s) inside %s";
                String name = attr.field.getType().getSimpleName();
                throw new IllegalStateException(String.format(fmt, name, name, typeValues.type.getSimpleName()));
            }
            attr.composeMapper();
        }
    }

    private BiConsumer<T, V> fillAll() {
        TypeValues.Attr[] attrs = this.attrs;
        int len = attrs.length;
        return (t, v) -> {
            int max = Math.min(len, sizeGetter.applyAsInt(v));
            for (int i = 0; i < max; i++) {
                fillAt(t, v, i, attrs[i]);
            }
        };
    }

    private BiConsumer<T, V> fillBySlots(int[] slots) {
        TypeValues.Attr[] attrs = this.attrs;
        int len = Math.min(attrs.length, slots.length);
        return (t, v) -> {
            int max = Math.min(len, sizeGetter.applyAsInt(v));
            for (int i = 0; i < max; i++) {
                fillAt(t, v, slots[i], attrs[i]);
            }
        };
    }

    private void fillAt(T t, V values, int index, TypeValues.Attr attr) {
        attr.set(t, convertAt(values, index, attr));
    }

    protected Object convertAt(V values, int index, TypeValues.Attr attr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T apply(V v) {
        try {
            T t = constructor.newInstance();
            filler.accept(t, v);
            return t;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Text<T> extends ValuesConverter<String[], T> {
        public Text(TypeValues<T> typeValues) {
            super(typeValues, a -> a.length);
        }

        @Override
        protected Object convertAt(String[] values, int index, TypeValues.Attr attr) {
            return attr.parse(values[index]);
        }

        public Function<String, T> toParser(String sep) {
            return compose(s -> s.split(sep));
        }
    }

    public static class Excel<T> extends ValuesConverter<Row, T> {
        public Excel(TypeValues<T> typeValues) {
            super(typeValues, Row::getLastCellNum);
            Map<Class<?>, Function<Cell, ?>> functionMap = new HashMap<>(9);
            functionMap.put(String.class, Cell::getStringCellValue);
            functionMap.put(boolean.class, Cell::getBooleanCellValue);
            functionMap.put(Boolean.class, Cell::getBooleanCellValue);
            functionMap.put(int.class, c -> (int)c.getNumericCellValue());
            functionMap.put(Integer.class, c -> (int)c.getNumericCellValue());
            functionMap.put(long.class, c -> (long)c.getNumericCellValue());
            functionMap.put(Long.class, c -> (long)c.getNumericCellValue());
            functionMap.put(double.class, Cell::getNumericCellValue);
            functionMap.put(Double.class, Cell::getNumericCellValue);
            DataFormatter df = new DataFormatter();
            for (TypeValues.Attr attr : attrs) {
                Function<Cell, ?> function = functionMap.get(attr.field.getType());
                if (function != null) {
                    attr.function = o -> function.apply((Cell)o);
                } else {
                    attr.function = o -> attr.parse(df.formatCellValue((Cell)o));
                }
            }
        }

        @Override
        protected Object convertAt(Row row, int index, TypeValues.Attr attr) {
            Cell cell = row.getCell(index);
            return attr.convert(cell);
        }
    }
}
