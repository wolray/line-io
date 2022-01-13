package com.github.wolray.line.io;

import java.util.function.Function;

/**
 * @author wolray
 */
public class DataMapper<T> {
    private final ValuesJoiner<T> joiner;
    private final ValuesConverter.Text<T> converter;
    private final Function<T, String> formatter;
    private final Function<String, T> parser;

    public DataMapper(Class<T> type) {
        this(type, "\u02cc");
    }

    public DataMapper(Class<T> type, String sep) {
        joiner = new ValuesJoiner<>(type);
        converter = new ValuesConverter.Text<>(type);
        formatter = toFormatter(sep);
        parser = toParser(sep);
    }

    public LineWriter<T> toWriter() {
        return new LineWriter<>(formatter);
    }

    public LineWriter<T> toWriter(String sep) {
        return new LineWriter<>(toFormatter(sep));
    }

    public LineReader.Text<T> toReader() {
        return new LineReader.Text<>(parser);
    }

    public CsvReader<T> toReader(String sep) {
        return new CsvReader<>(converter, sep);
    }

    public Function<T, String> toFormatter(String sep) {
        return joiner.toFormatter(sep);
    }

    public Function<String, T> toParser(String sep) {
        return converter.toParser(sep);
    }

    public String format(T t) {
        return formatter.apply(t);
    }

    public T parse(String str) {
        return parser.apply(str);
    }
}
