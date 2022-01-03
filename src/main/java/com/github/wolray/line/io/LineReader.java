package com.github.wolray.line.io;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author ray
 */
public class LineReader<S, V, T> {
    protected final Class<T> type;
    protected final Function<V, T> function;

    private LineReader(Class<T> type, Function<V, T> function) {
        this.type = type;
        this.function = function;
    }

    public static <T> Text<T> simple(Class<T> type, Function<String, T> parser) {
        return new Text<>(type, parser);
    }

    public static <T> Csv<T> byCsv(String sep, Class<T> type) {
        return new Csv<>(new ValuesConverter.Text<>(type), sep);
    }

    public static <T> Excel<T> byExcel(Class<T> type) {
        return byExcel(0, type);
    }

    public static <T> Excel<T> byExcel(int sheetIndex, Class<T> type) {
        return new Excel<>(sheetIndex, new ValuesConverter.Excel<>(type));
    }

    public static InputStream toInputStream(String file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public final DataStream<T> read(S source) {
        return read(source, 0, null);
    }

    public final DataStream<T> read(S source, int skipLines) {
        return read(source, skipLines, null);
    }

    public final DataStream<T> read(S source, int skipLines, Columns columns) {
        return new DataStream<>(type, () -> {
            Stream<V> stream = toStream(source).skip(skipLines);
            if (columns != null && columns.slots.length > 0) {
                reorder(columns.slots);
            }
            return stream.map(function);
        });
    }

    protected Stream<V> toStream(S source) {
        throw new UnsupportedOperationException();
    }

    protected void reorder(int[] slots) {
        throw new UnsupportedOperationException();
    }

    public static class Text<T> extends LineReader<InputStream, String, T> {
        private Text(Class<T> type, Function<String, T> parser) {
            super(type, parser);
        }

        @Override
        protected Stream<String> toStream(InputStream source) {
            return new BufferedReader(new InputStreamReader(source)).lines();
        }
    }

    public static class Csv<T> extends Text<T> {
        private final ValuesConverter.Text<T> converter;
        private final String sep;

        private Csv(ValuesConverter.Text<T> converter, String sep) {
            super(converter.type, converter.compose(s -> s.split(sep)));
            this.converter = converter;
            this.sep = sep;
        }

        @Override
        protected void reorder(int[] slots) {
            converter.resetOrder(slots);
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

        public final DataStream<T> read(InputStream is, int skipLines, String... header) {
            return new DataStream<>(type, () -> {
                Stream<String> stream = toStream(is).skip(skipLines);
                return StreamHelper.consumeFirst(stream,
                    s -> setHeader(s, header), function);
            });
        }
    }

    public static class Excel<T> extends LineReader<InputStream, Row, T> {
        private final int sheetIndex;

        private Excel(int sheetIndex, ValuesConverter.Excel<T> converter) {
            super(converter.type, converter);
            this.sheetIndex = sheetIndex;
        }

        @Override
        protected Stream<Row> toStream(InputStream source) {
            try {
                Workbook workbook = new XSSFWorkbook(source);
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                return StreamHelper.toStream(sheet.iterator(), null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        protected void reorder(int[] slots) {
            ((ValuesConverter.Excel<T>)function).resetOrder(slots);
        }
    }
}
