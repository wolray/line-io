package com.github.wolray.line.io;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author wolray
 */
public class CsvWriter<T> extends LineWriter<T> {
    private final ValuesJoiner<T> joiner;
    private final String sep;

    CsvWriter(ValuesJoiner<T> joiner, String sep) {
        super(joiner.toFormatter(sep));
        this.joiner = joiner;
        this.sep = sep;
    }

    @Override
    public Session write(Iterable<T> iterable) {
        return new Session(iterable);
    }

    public class Session extends LineWriter<T>.Session {
        private boolean utf8;

        protected Session(Iterable<T> iterable) {
            super(iterable);
        }

        public Session markUtf8() {
            utf8 = true;
            return this;
        }

        public Session withHeader() {
            addHeader(joiner.join(sep, a -> a.field.getName()));
            return this;
        }

        public Session columns(String... columns) {
            if (columns.length > 0) {
                addHeader(String.join(sep, columns));
            }
            return this;
        }

        @Override
        protected void preprocess(String file, BufferedWriter bw) throws IOException {
            if (utf8 && file.endsWith(".csv")) {
                bw.write('\ufeff');
            }
        }
    }
}
