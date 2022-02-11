package com.github.wolray.line.io;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author wolray
 */
public class DataMapper<T> {
    public final TypeValues<T> typeValues;
    public final String sep;
    private ValuesConverter.Text<T> converter;
    private ValuesJoiner<T> joiner;
    private Function<String, T> parser;
    private Function<T, String> formatter;

    public DataMapper(Class<T> type) {
        this(new TypeValues<>(type));
    }

    public DataMapper(Class<T> type, String sep) {
        this(new TypeValues<>(type), sep);
    }

    public DataMapper(TypeValues<T> typeValues) {
        this(typeValues, "\u02cc");
    }

    public DataMapper(TypeValues<T> typeValues, String sep) {
        this.typeValues = Objects.requireNonNull(typeValues);
        this.sep = Objects.requireNonNull(sep);
    }

    public static <T> DataMapper.Builder<T> of(Class<T> type) {
        return new Builder<>(type);
    }

    public DataMapper<T> newSep(String sep) {
        if (sep.equals(this.sep)) {
            return this;
        }
        return new DataMapper<>(typeValues, sep);
    }

    public ValuesConverter.Text<T> getConverter() {
        if (converter == null) {
            converter = new ValuesConverter.Text<>(typeValues);
        }
        return converter;
    }

    public ValuesJoiner<T> getJoiner() {
        if (joiner == null) {
            joiner = new ValuesJoiner<>(typeValues);
        }
        return joiner;
    }

    public Function<String, T> toParser(String sep) {
        return getConverter().toParser(sep);
    }

    public Function<T, String> toFormatter(String sep) {
        return getJoiner().toFormatter(sep);
    }

    public CsvReader<T> toReader() {
        return toReader(sep);
    }

    public CsvReader<T> toReader(String sep) {
        return new CsvReader<>(getConverter(), sep);
    }

    public CsvWriter<T> toWriter() {
        return toWriter(sep);
    }

    public CsvWriter<T> toWriter(String sep) {
        return new CsvWriter<>(getJoiner(), sep);
    }

    public T parse(String s) {
        try {
            return parser.apply(s);
        } catch (NullPointerException e) {
            if (parser != null) {
                throw e;
            }
            parser = toParser(sep);
            return parser.apply(s);
        }
    }

    public String format(T t) {
        try {
            return formatter.apply(t);
        } catch (NullPointerException e) {
            if (formatter != null) {
                throw e;
            }
            formatter = toFormatter(sep);
            return formatter.apply(t);
        }
    }

    public static class Builder<T> {
        private final Class<T> type;
        private final FieldSelector selector = new FieldSelector();
        private String sep = "\u02cc";

        private Builder(Class<T> type) {
            this.type = type;
        }

        public Builder<T> pojo() {
            selector.pojo = true;
            return this;
        }

        public Builder<T> use(String... fields) {
            selector.use = fields;
            return this;
        }

        public Builder<T> omit(String... fields) {
            selector.omit = fields;
            return this;
        }

        public Builder<T> useRegex(String fieldsRegex) {
            selector.useRegex = fieldsRegex;
            return this;
        }

        public Builder<T> omitRegex(String fieldsRegex) {
            selector.omitRegex = fieldsRegex;
            return this;
        }

        public Builder<T> sep(String sep) {
            this.sep = sep;
            return this;
        }

        public DataMapper<T> build() {
            return new DataMapper<>(new TypeValues<>(type, selector), sep);
        }
    }
}
