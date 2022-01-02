package com.github.wolray.line.io;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * @author ray
 */
public class ValuesConverter<V, T> extends ValuesBase<T> implements Function<V, T> {
    private final ToIntFunction<V> sizeGetter;
    private final Constructor<T> constructor;
    private BiConsumer<T, V> filler;

    public ValuesConverter(Class<T> type, ToIntFunction<V> sizeGetter) {
        super(type);
        this.sizeGetter = sizeGetter;
        constructor = initConstructor(type);
        initConverters();
        filler = fillAll();
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

    private void initConverters() {
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
        for (FieldContext context : fieldContexts) {
            context.parser = parserMap.get(context.field.getType());
        }
        processStaticMethods();
        checkParsers();
    }

    @Override
    void processMethod(Method method, Class<?> paraType, Class<?> returnType) {
        if (paraType == String.class) {
            Stream<FieldContext> fields = filterFields(method.getAnnotation(Fields.class));
            method.setAccessible(true);
            if (returnType == String.class) {
                UnaryOperator<String> mapper = s -> (String)invoke(method, s);
                fields.forEach(c -> c.mapper = mapper);
            } else {
                Function<String, Object> parser = s -> invoke(method, s);
                fields
                    .filter(c -> c.field.getType() == returnType)
                    .forEach(c -> c.parser = parser);
            }
        }
    }

    private void checkParsers() {
        for (FieldContext context : fieldContexts) {
            if (context.parser == null) {
                String fmt = "cannot parse %s\n\tplease add a static method (String -> %s) in %s or its parent classes";
                String name = context.field.getType().getSimpleName();
                throw new IllegalStateException(String.format(fmt, name, name, type.getSimpleName()));
            }
            context.composeMapper();
        }
    }

    private BiConsumer<T, V> fillAll() {
        int len = fieldContexts.length;
        return (t, v) -> {
            int max = Math.min(len, sizeGetter.applyAsInt(v));
            for (int i = 0; i < max; i++) {
                fillAt(t, v, i, fieldContexts[i]);
            }
        };
    }

    private BiConsumer<T, V> fillBySlots(int[] slots) {
        int len = Math.min(fieldContexts.length, slots.length);
        return (t, v) -> {
            int max = Math.min(len, sizeGetter.applyAsInt(v));
            for (int i = 0; i < max; i++) {
                fillAt(t, v, slots[i], fieldContexts[i]);
            }
        };
    }

    private void fillAt(T t, V values, int index, FieldContext context) {
        context.set(t, convertAt(values, index, context));
    }

    protected Object convertAt(V values, int index, FieldContext context) {
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
        public Text(Class<T> type) {
            super(type, a -> a.length);
        }

        @Override
        protected Object convertAt(String[] values, int index, FieldContext context) {
            return context.convertString(values[index]);
        }
    }

    public static class Excel<T> extends ValuesConverter<Row, T> {
        public Excel(Class<T> type) {
            super(type, Row::getLastCellNum);
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
            for (FieldContext context : fieldContexts) {
                Function<Cell, ?> function = functionMap.get(context.field.getType());
                if (function != null) {
                    context.function = o -> function.apply((Cell)o);
                } else {
                    context.function = o -> context.convertString(df.formatCellValue((Cell)o));
                }
            }
        }

        @Override
        protected Object convertAt(Row row, int index, FieldContext context) {
            Cell cell = row.getCell(index);
            return context.convertObject(cell);
        }
    }
}
