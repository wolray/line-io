package com.github.wolray.line.io;

import java.io.InputStream;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ray
 */
public class DataStream<T> {
    private List<T> ts;
    private Supplier<Stream<T>> supplier;

    DataStream(List<T> ts) {
        set(ts);
    }

    DataStream(Supplier<Stream<T>> supplier) {
        this.supplier = supplier;
    }

    public static <T> DataStream<T> of(Collection<T> ts) {
        if (ts instanceof List) {
            return new DataStream<>((List<T>)ts);
        } else {
            return new DataStream<>(ts::stream);
        }
    }

    public static <T> DataStream<T> of(Supplier<Stream<T>> supplier) {
        return new DataStream<>(supplier);
    }

    private void set(List<T> list) {
        ts = list;
        supplier = list::stream;
    }

    public boolean isReusable() {
        return ts != null;
    }

    private DataStream<T> mod(UnaryOperator<Stream<T>> op) {
        Supplier<Stream<T>> old = supplier, next = () -> op.apply(old.get());
        if (isReusable()) {
            return new DataStream<>(next);
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

    public <E> DataStream<E> map(Function<T, E> mapper) {
        Supplier<Stream<T>> old = supplier;
        return new DataStream<>(() -> old.get().map(mapper));
    }

    public DataStream<T> reuse() {
        if (!isReusable()) {
            set(supplier.get().collect(Collectors.toCollection(DataList::new)));
        }
        return this;
    }

    public DataStream<T> cacheBy(Cache<T> cache) {
        if (cache.exists()) {
            return cache.getReader().get();
        } else {
            cache.getWriter().accept(toList());
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
            public Supplier<DataStream<T>> getReader() {
                return reader.get().read(is)::stream;
            }

            @Override
            public Consumer<List<T>> getWriter() {
                return ts -> writer.get().writeAsync(ts, f);
            }
        });
    }

    public DataStream<T> cacheCsv(String file, Class<T> type) {
        return cacheCsv(file, type, ",");
    }

    public DataStream<T> cacheCsv(String file, Class<T> type, String sep) {
        return cacheFile(file, ".csv",
            () -> LineReader.byCsv(sep, type),
            () -> LineWriter.byCsv(sep, type));
    }

    public DataStream<T> cacheJson(String file, Class<T> type) {
        return cacheFile(file, ".txt",
            () -> LineReader.byJson(type),
            LineWriter::byJson);
    }

    public boolean isEmpty() {
        return toList().isEmpty();
    }

    public void forEach(Consumer<T> action) {
        if (isReusable()) {
            ts.forEach(action);
        } else {
            supplier.get().forEach(action);
        }
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

        Supplier<DataStream<T>> getReader();

        Consumer<List<T>> getWriter();
    }
}
