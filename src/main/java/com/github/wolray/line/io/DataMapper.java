package com.github.wolray.line.io;

import java.util.function.Function;

/**
 * @author wolray
 */
public class DataMapper<T> {
    private final ValuesFormatter<T> formatter;
    private final ValuesConverter.Text<T> converter;
    private final Function<T, String> printer;
    private final Function<String, T> parser;

    public DataMapper(Class<T> type) {
        this(type, "\u02cc");
    }

    public DataMapper(Class<T> type, String sep) {
        formatter = new ValuesFormatter<>(type);
        converter = new ValuesConverter.Text<>(type);
        printer = toPrinter(sep);
        parser = toParser(sep);
    }

    public LineWriter<T> toWriter() {
        return new LineWriter<>(printer);
    }

    public LineWriter<T> toWriter(String sep) {
        return new LineWriter<>(toPrinter(sep));
    }

    public LineReader.Text<T> toReader() {
        return new LineReader.Text<>(parser);
    }

    public CsvReader<T> toReader(String sep) {
        return new CsvReader<>(converter, sep);
    }

    public Function<T, String> toPrinter(String sep) {
        return formatter.toPrinter(sep);
    }

    public Function<String, T> toParser(String sep) {
        return converter.toParser(sep);
    }

    public String format(T t) {
        return printer.apply(t);
    }

    public T parse(String str) {
        return parser.apply(str);
    }
}
