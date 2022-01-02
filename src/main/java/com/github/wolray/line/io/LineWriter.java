package com.github.wolray.line.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

/**
 * @author ray
 */
public class LineWriter<T> {
    private final Function<T, String> formatter;
    private String header;
    private boolean markCsvAsUTF8;
    private boolean append;

    public LineWriter(Function<T, String> formatter) {
        this.formatter = formatter;
    }

    public static <T> LineWriter<T> byCsv(String sep, Class<T> type, boolean withHeader) {
        ValuesFormatter<T> formatter = new ValuesFormatter<>(type, sep);
        LineWriter<T> res = new LineWriter<>(formatter);
        if (withHeader) {
            res.header(formatter.joinFields(c -> c.field.getName()));
        }
        return res;
    }

    public static <T> LineWriter<T> byCsv(String sep, Class<T> type, String... columns) {
        ValuesFormatter<T> formatter = new ValuesFormatter<>(type, sep);
        LineWriter<T> res = new LineWriter<>(formatter);
        if (columns.length > 0) {
            res.header(String.join(sep, columns));
        }
        return res;
    }

    public LineWriter<T> header(String header) {
        this.header = header;
        return this;
    }

    public LineWriter<T> markCsvAsUTF8() {
        markCsvAsUTF8 = true;
        return this;
    }

    public LineWriter<T> appendToFile() {
        append = true;
        return this;
    }

    public void write(Iterable<T> iterable, String file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, append))) {
            if (!append) {
                if (markCsvAsUTF8 && file.endsWith(".csv")) {
                    bw.write('\ufeff');
                }
                if (header != null) {
                    bw.write(header);
                    bw.write('\n');
                }
            }
            Function<T, String> formatter = this.formatter;
            for (T t : iterable) {
                bw.write(formatter.apply(t));
                bw.write('\n');
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
