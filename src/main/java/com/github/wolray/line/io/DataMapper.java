package com.github.wolray.line.io;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

    public static synchronized <T> void scan(Class<T> clazz) {
        try {
            for (Field f : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    Class<?> type = f.getType();
                    if (type == DataMapper.class) {
                        TypeValues<?> typeValues = makeTypeValues(f);
                        f.setAccessible(true);
                        Object value = f.get(null);
                        if (value == null) {
                            f.set(null, new DataMapper<>(typeValues));
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static TypeValues<?> makeTypeValues(Field f) {
        Type genericType = f.getGenericType();
        if (genericType instanceof ParameterizedType) {
            Type argument = ((ParameterizedType)genericType).getActualTypeArguments()[0];
            Class<?> type = (Class<?>)argument;
            Fields fields = f.getAnnotation(Fields.class);
            if (fields != null) {
                return new TypeValues<>(type, fields);
            } else {
                return new TypeValues<>(type);
            }
        }
        throw new IllegalStateException("unset DataMapper type");
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
}
