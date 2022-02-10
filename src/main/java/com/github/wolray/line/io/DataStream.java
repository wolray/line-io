package com.github.wolray.line.io;

import java.io.InputStream;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author wolray
 */
public class DataStream<T> {
    private List<T> ts;
    private Supplier<Stream<T>> supplier;

    private DataStream(List<T> ts) {
        setList(ts);
    }

    private DataStream(Supplier<Stream<T>> supplier) {
        this.supplier = supplier;
    }

    public static <T> DataStream<T> of(Collection<T> ts) {
        if (ts instanceof List) {
            return new DataStream<>((List<T>)ts);
        } else {
            return of(ts::stream);
        }
    }

    public static <T> DataStream<T> of(Supplier<Stream<T>> supplier) {
        return new DataStream<>(supplier);
    }

    public static <T> DataStream<T> empty() {
        return of(Stream::empty);
    }

    private void setList(List<T> list) {
        ts = list;
        supplier = list::stream;
    }

    public boolean isReusable() {
        return ts != null;
    }

    public DataStream<T> reuse() {
        if (!isReusable()) {
            setList(supplier.get().collect(Collectors.toCollection(DataList::new)));
        }
        return this;
    }

    private DataStream<T> mod(UnaryOperator<Stream<T>> op) {
        Supplier<Stream<T>> old = supplier, next = () -> op.apply(old.get());
        if (isReusable()) {
            return of(next);
        } else {
            supplier = next;
            return this;
        }
    }

    public DataStream<T> limit(int maxSize) {
        return mod(s -> s.limit(maxSize));
    }

    public DataStream<T> peek(Consumer<T> action) {
        return mod(s -> s.peek(action));
    }

    public DataStream<T> filter(Predicate<T> predicate) {
        return mod(s -> s.filter(predicate));
    }

    public DataStream<T> consumeIf(boolean condition, Consumer<DataStream<T>> consumer) {
        if (condition) {
            consumer.accept(this);
            return isReusable() ? this : empty();
        }
        return this;
    }

    public DataStream<T> operateIf(boolean condition, UnaryOperator<DataStream<T>> op) {
        return condition ? op.apply(this) : this;
    }

    public DataStream<T> cacheBy(Cache<T> cache) {
        if (cache.exists()) {
            return cache.read();
        } else {
            List<T> ts = toList();
            if (!ts.isEmpty()) {
                cache.write(ts);
            }
            return this;
        }
    }

    private DataStream<T> cacheFile(String file, String suffix,
        Supplier<LineReader.Text<T>> reader, Supplier<LineWriter<T>> writer) {
        String f = file.endsWith(suffix) ? file : (file + suffix);
        InputStream is = LineReader.toInputStream(f);
        return cacheBy(new Cache<T>() {
            @Override
            public boolean exists() {
                return is != null;
            }

            @Override
            public DataStream<T> read() {
                return reader.get().read(is).stream();
            }

            @Override
            public void write(List<T> ts) {
                writer.get().write(ts, f);
            }
        });
    }

    public DataStream<T> cacheCsv(String file, Class<T> type) {
        return cacheCsv(file, type, ",");
    }

    public DataStream<T> cacheCsv(String file, Class<T> type, String sep) {
        DataMapper<T> mapper = new DataMapper<>(type, sep);
        return cacheCsv(file, mapper);
    }

    public DataStream<T> cacheCsv(String file, DataMapper<T> mapper) {
        return cacheFile(file, ".csv", mapper::toReader, mapper::toWriter);
    }

    public DataStream<T> cacheJson(String file, Class<T> type) {
        return cacheFile(file, ".txt",
            () -> LineReader.byJson(type),
            LineWriter::byJson);
    }

    public <E> DataStream<E> map(Function<T, E> mapper) {
        Supplier<Stream<T>> old = supplier;
        return of(() -> old.get().map(mapper));
    }

    public void forEach(Consumer<T> action) {
        if (isReusable()) {
            ts.forEach(action);
        } else {
            supplier.get().forEach(action);
        }
    }

    public void parallelFor(Consumer<T> action) {
        toList().parallelStream().forEach(action);
    }

    public List<T> toList() {
        reuse();
        return ts;
    }

    public List<T> toArrayList() {
        return new ArrayList<>(toList());
    }

    public <K> Set<K> toSet(Function<T, K> mapper) {
        List<T> ts = toList();
        Set<K> set = new HashSet<>(ts.size());
        ts.forEach(t -> set.add(mapper.apply(t)));
        return set;
    }

    public <K, V> Map<K, V> toMap(Function<T, K> keyMapper, Function<T, V> valueMapper) {
        List<T> ts = toList();
        Map<K, V> map = new HashMap<>(ts.size());
        ts.forEach(t -> map.put(keyMapper.apply(t), valueMapper.apply(t)));
        return map;
    }

    public <K, V> Map<K, V> groupBy(Function<T, K> keyMapper, Collector<T, ?, V> collector) {
        return supplier.get().collect(Collectors.groupingBy(keyMapper, collector));
    }

    public interface Cache<T> {
        boolean exists();

        DataStream<T> read();

        void write(List<T> ts);
    }
}
