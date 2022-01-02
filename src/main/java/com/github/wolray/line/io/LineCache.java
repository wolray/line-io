package com.github.wolray.line.io;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author ray
 */
public class LineCache<T> {
    private final LineReader.Text<T> reader;
    private final LineWriter<T> writer;

    private LineCache(String sep, Class<T> type) {
        if (sep != null) {
            reader = LineReader.byCsv(sep, type);
            writer = LineWriter.byCsv(sep, type);
        } else {
            reader = LineReader.byJson(type);
            writer = LineWriter.byJson();
        }
    }

    public static <T> LineCache<T> byCsv(String sep, Class<T> type) {
        return new LineCache<>(Objects.requireNonNull(sep), type);
    }

    public static <T> LineCache<T> byJson(Class<T> type) {
        return new LineCache<>(null, type);
    }

    public List<T> get(String filePrefix, Supplier<List<T>> supplier) {
        String file = filePrefix + ".csv";
        InputStream is = LineReader.toInputStream(file);
        if (is != null) {
            return reader.read(is).toList();
        }
        List<T> res = supplier.get();
        writer.write(res, file);
        return res;
    }
}
