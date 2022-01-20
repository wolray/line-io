package com.github.wolray.line.io;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author ray
 */
public class CsvReader<T> extends LineReader.Text<T> {
    private final ValuesConverter.Text<T> converter;
    private final String sep;

    CsvReader(ValuesConverter.Text<T> converter, String sep) {
        super(converter.toParser(sep));
        this.converter = converter;
        this.sep = sep;
    }

    @Override
    protected void reorder(int[] slots) {
        converter.resetOrder(slots);
    }

    @Override
    public Session read(InputStream is) {
        return read(is, 0);
    }

    @Override
    public Session read(InputStream is, int skipLines) {
        return new Session(is, skipLines);
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

    public class Session extends Text<T>.Session {
        private String[] cols;

        public Session(InputStream is, int skipLines) {
            super(is, skipLines);
        }

        public Session csvHeader(String... cols) {
            this.cols = cols;
            return this;
        }

        @Override
        protected void preprocess(Iterator<String> iterator) {
            if (cols != null && cols.length > 0) {
                setHeader(iterator.next(), cols);
            } else {
                super.preprocess(iterator);
            }
        }
    }
}
