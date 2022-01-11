package com.github.wolray.line.io;

import java.util.function.Function;

/**
 * @author wolray
 */
public class DataMapper<T> {
    private final ValuesFormatter<T> formatter;
    private final Function<String, T> converter;

    public DataMapper(Class<T> type, String sep) {
        formatter = new ValuesFormatter<>(type, sep);
        converter = new ValuesConverter.Text<>(type).compose(s -> s.split(sep));
    }

    public T parse(String str) {
        return converter.apply(str);
    }

    public String format(T t) {
        return formatter.apply(t);
    }
}
