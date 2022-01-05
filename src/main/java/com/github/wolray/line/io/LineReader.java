package com.github.wolray.line.io;

import com.alibaba.fastjson.JSON;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author ray
 */
public class LineReader<S, V, T> {
    protected final Function<V, T> function;

    protected LineReader(Function<V, T> function) {
        this.function = function;
    }

    public static <T> Text<T> simple(Function<String, T> parser) {
        return new Text<>(parser);
    }

    public static <T> Text<T> byJson(Class<T> type) {
        return new Text<>(s -> JSON.parseObject(s, type));
    }

    public static <T> CsvReader<T> byCsv(String sep, Class<T> type) {
        return new CsvReader<>(new ValuesConverter.Text<>(type), sep);
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

    public Session read(S source) {
        return read(source, 0);
    }

    public Session read(S source, int skipLines) {
        return new Session(source, skipLines);
    }

    protected Stream<V> toStream(S source) {
        throw new UnsupportedOperationException();
    }

    protected void reorder(int[] slots) {
        throw new UnsupportedOperationException();
    }

    public static class Text<T> extends LineReader<InputStream, String, T> {
        Text(Function<String, T> parser) {
            super(parser);
        }

        @Override
        protected Stream<String> toStream(InputStream source) {
            return new BufferedReader(new InputStreamReader(source)).lines();
        }
    }

    public static class Excel<T> extends LineReader<InputStream, Row, T> {
        private final int sheetIndex;

        private Excel(int sheetIndex, ValuesConverter.Excel<T> converter) {
            super(converter);
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

    public class Session {
        private final S source;
        private final int skipLines;
        private int[] slots;

        protected Session(S source, int skipLines) {
            this.source = source;
            this.skipLines = skipLines;
        }

        public Session columns(int... slots) {
            this.slots = slots;
            return this;
        }

        public Session columns(String excelCols) {
            if (excelCols == null || excelCols.isEmpty()) {
                slots = new int[0];
            } else {
                String[] split = excelCols.split(",");
                slots = new int[split.length];
                char a = 'A';
                for (int i = 0; i < split.length; i++) {
                    String col = split[i].trim();
                    int j = col.charAt(0) - a;
                    if (col.length() > 1) {
                        slots[i] = (j + 1) * 26 + col.charAt(1) - a;
                    } else {
                        slots[i] = j;
                    }
                }
            }
            return this;
        }

        protected Stream<T> map(Stream<V> stream) {
            if (slots != null && slots.length > 0) {
                reorder(slots);
            }
            return stream.map(function);
        }

        public DataStream<T> stream() {
            return new DataStream<>(() -> map(toStream(source).skip(skipLines)));
        }
    }
}
