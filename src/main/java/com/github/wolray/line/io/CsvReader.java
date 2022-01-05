package com.github.wolray.line.io;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * @author ray
 */
public class CsvReader<T> extends LineReader.Text<T> {
    private final ValuesConverter.Text<T> converter;
    private final String sep;

    CsvReader(ValuesConverter.Text<T> converter, String sep) {
        super(converter.compose(s -> s.split(sep)));
        this.converter = converter;
        this.sep = sep;
    }

    @Override
    protected void reorder(int[] slots) {
        converter.resetOrder(slots);
    }

    @Override
    public Session read(InputStream source, int skipLines) {
        return new Session(source, skipLines);
    }

    private void setHeader(String s, String[] header) {
        List<String> list = Arrays.asList(s.split(sep));
        int[] slots = Arrays.stream(header)
            .mapToInt(list::indexOf)
            .toArray();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] < 0) {
                throw new NoSuchElementException(header[i]);
            }
        }
        reorder(slots);
    }

    public class Session extends LineReader.Text<T>.Session {
        private String[] cols;

        public Session(InputStream is, int skipLines) {
            super(is, skipLines);
        }

        public Session csvHeader(String... cols) {
            this.cols = cols;
            return this;
        }

        @Override
        protected Stream<T> map(Stream<String> stream) {
            if (cols != null && cols.length > 0) {
                return StreamHelper.consumeFirst(stream,
                    s -> setHeader(s, cols), function);
            }
            return super.map(stream);
        }
    }
}
