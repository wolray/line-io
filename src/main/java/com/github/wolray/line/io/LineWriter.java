package com.github.wolray.line.io;

import com.alibaba.fastjson.JSON;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author wolray
 */
public class LineWriter<T> {
    private final Function<T, String> formatter;

    public LineWriter(Function<T, String> formatter) {
        this.formatter = formatter;
    }

    public static <T> LineWriter<T> byJson() {
        return new LineWriter<>(JSON::toJSONString);
    }

    public static <T> CsvWriter<T> byCsv(String sep, Class<T> type) {
        return new CsvWriter<>(new ValuesJoiner<>(new TypeValues<>(type)), sep);
    }

    public Session write(String file) {
        return new Session(file);
    }

    public class Session {
        protected final String file;
        private final List<String> headers = new LinkedList<>();
        private boolean append;

        protected Session(String file) {
            this.file = file;
        }

        public Session addHeader(String header) {
            headers.add(header);
            return this;
        }

        public Session appendToFile() {
            append = true;
            return this;
        }

        protected void preprocess(BufferedWriter bw) throws IOException {}

        public void asyncWith(Iterable<T> iterable) {
            CompletableFuture.runAsync(() -> with(iterable));
        }

        public void with(Iterable<T> iterable) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, append))) {
                if (!append) {
                    preprocess(bw);
                    for (String header : headers) {
                        bw.write(header);
                        bw.newLine();
                    }
                }
                for (T t : iterable) {
                    bw.write(formatter.apply(t));
                    bw.newLine();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
