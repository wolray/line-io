package com.github.wolray.line.io;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author ray
 */
public class LineCache<T> {
    private final LineReader.Text<T> reader;
    private final LineWriter<T> writer;

    public LineCache(String sep, Class<T> type) {
        reader = LineReader.byCsv(sep, type);
        writer = LineWriter.byCsv(sep, type);
    }

    public List<T> get(String file, Supplier<List<T>> supplier) {
        if (!file.endsWith(".csv")) {
            file = file + ".csv";
        }
        InputStream is = LineReader.toInputStream(file);
        if (is != null) {
            return reader.read(is).toList();
        }
        List<T> res = supplier.get();
        writer.writeAsync(res, file);
        return res;
    }
}
