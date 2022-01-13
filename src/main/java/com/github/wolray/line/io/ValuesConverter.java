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

import static com.github.wolray.line.io.TypeData.invoke;

/**
 * @author ray
 */
public class ValuesConverter<V, T> implements Function<V, T> {
    protected final TypeData<T> typeData;
    protected final TypeData.Attr[] attrs;
    private final ToIntFunction<V> sizeGetter;
    private final Constructor<T> constructor;
    private BiConsumer<T, V> filler;

    public ValuesConverter(TypeData<T> typeData, ToIntFunction<V> sizeGetter) {
        this.typeData = typeData;
        this.sizeGetter = sizeGetter;
        attrs = typeData.toAttrs();
        constructor = initConstructor(typeData.type);
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

        for (TypeData.Attr data : attrs) {
            Class<?> type = data.field.getType();
            if (type.isEnum()) {
                data.parser = s -> parseEnum(type, s);
            } else {
                data.parser = parserMap.get(type);
            }
        }
        TypeData.processSimpleMethods(typeData.type, this::processMethod);
        checkParsers();
    }

    void processMethod(TypeData.SimpleMethod simpleMethod) {
        Method method = simpleMethod.method;
        Class<?> returnType = simpleMethod.returnType;
        if (simpleMethod.paraType == String.class) {
            Fields fields = method.getAnnotation(Fields.class);
            Predicate<Field> predicate = TypeData.makePredicate(fields);
            Stream<TypeData.Attr> stream = Arrays.stream(attrs)
                .filter(a -> predicate.test(a.field));
            method.setAccessible(true);
            if (returnType == String.class) {
                UnaryOperator<String> mapper = s -> (String)invoke(method, s);
                stream.forEach(c -> c.mapper = mapper);
            } else {
                Function<String, Object> parser = s -> invoke(method, s);
                stream
                    .filter(c -> c.field.getType() == returnType)
                    .forEach(c -> c.parser = parser);
            }
        }
    }

    private void checkParsers() {
        for (TypeData.Attr data : attrs) {
            if (data.parser == null) {
                String fmt = "cannot parse %s, please add a static method (String -> %s) inside %s";
                String name = data.field.getType().getSimpleName();
                throw new IllegalStateException(String.format(fmt, name, name, typeData.type.getSimpleName()));
            }
            data.composeMapper();
        }
    }

    private BiConsumer<T, V> fillAll() {
        TypeData.Attr[] data = attrs;
        int len = data.length;
        return (t, v) -> {
            int max = Math.min(len, sizeGetter.applyAsInt(v));
            for (int i = 0; i < max; i++) {
                fillAt(t, v, i, data[i]);
            }
        };
    }

    private BiConsumer<T, V> fillBySlots(int[] slots) {
        TypeData.Attr[] data = attrs;
        int len = Math.min(data.length, slots.length);
        return (t, v) -> {
            int max = Math.min(len, sizeGetter.applyAsInt(v));
            for (int i = 0; i < max; i++) {
                fillAt(t, v, slots[i], data[i]);
            }
        };
    }

    private void fillAt(T t, V values, int index, TypeData.Attr context) {
        context.set(t, convertAt(values, index, context));
    }

    protected Object convertAt(V values, int index, TypeData.Attr context) {
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
        public Text(TypeData<T> typeData) {
            super(typeData, a -> a.length);
        }

        @Override
        protected Object convertAt(String[] values, int index, TypeData.Attr context) {
            return context.parse(values[index]);
        }

        public Function<String, T> toParser(String sep) {
            return compose(s -> s.split(sep));
        }
    }

    public static class Excel<T> extends ValuesConverter<Row, T> {
        public Excel(TypeData<T> typeData) {
            super(typeData, Row::getLastCellNum);
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
            for (TypeData.Attr data : attrs) {
                Function<Cell, ?> function = functionMap.get(data.field.getType());
                if (function != null) {
                    data.function = o -> function.apply((Cell)o);
                } else {
                    data.function = o -> data.parse(df.formatCellValue((Cell)o));
                }
            }
        }

        @Override
        protected Object convertAt(Row row, int index, TypeData.Attr data) {
            Cell cell = row.getCell(index);
            return data.convert(cell);
        }
    }
}
