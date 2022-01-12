package com.github.wolray.line.io;

import com.alibaba.fastjson.JSON;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author ray
 */
public class LineWriter<T> {
    private final Function<T, String> printer;
    private String header;
    private boolean markCsvAsUtf8;
    private boolean append;

    public LineWriter(Function<T, String> printer) {
        this.printer = printer;
    }

    public static <T> LineWriter<T> byJson() {
        return new LineWriter<>(JSON::toJSONString);
    }

    public static <T> LineWriter<T> byCsv(String sep, Class<T> type, boolean withHeader) {
        ValuesFormatter<T> formatter = new ValuesFormatter<>(type);
        LineWriter<T> res = new LineWriter<>(formatter.toPrinter(sep));
        if (withHeader) {
            res.header(formatter.join(sep, c -> c.field.getName()));
        }
        return res;
    }

    public static <T> LineWriter<T> byCsv(String sep, Class<T> type, String... columns) {
        ValuesFormatter<T> formatter = new ValuesFormatter<>(type);
        LineWriter<T> res = new LineWriter<>(formatter.toPrinter(sep));
        if (columns.length > 0) {
            res.header(String.join(sep, columns));
        }
        return res;
    }

    public LineWriter<T> header(String header) {
        this.header = header;
        return this;
    }

    public LineWriter<T> markCsvAsUtf8() {
        markCsvAsUtf8 = true;
        return this;
    }

    public LineWriter<T> appendToFile() {
        append = true;
        return this;
    }

    public void writeAsync(Iterable<T> iterable, String file) {
        CompletableFuture.runAsync(() -> write(iterable, file));
    }

    public void write(Iterable<T> iterable, String file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, append))) {
            if (!append) {
                if (markCsvAsUtf8 && file.endsWith(".csv")) {
                    bw.write('\ufeff');
                }
                if (header != null) {
                    bw.write(header);
                    bw.write('\n');
                }
            }
            Function<T, String> printer = this.printer;
            for (T t : iterable) {
                bw.write(printer.apply(t));
                bw.write('\n');
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
