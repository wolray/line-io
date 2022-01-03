package com.github.wolray.line.io;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ray
 */
public class DataStream<T> {
    private final Class<T> type;
    private Supplier<Stream<T>> supplier;

    DataStream(Class<T> type, Supplier<Stream<T>> supplier) {
        this.type = type;
        this.supplier = supplier;
    }

    private DataStream<T> mod(UnaryOperator<Stream<T>> op) {
        Supplier<Stream<T>> old = supplier;
        supplier = () -> op.apply(old.get());
        return this;
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

    public void forEach(Consumer<T> action) {
        supplier.get().forEach(action);
    }

    public List<T> toList() {
        return supplier.get().collect(Collectors.toCollection(LinkedList::new));
    }

    public List<T> toList(String cacheCsvFile) {
        return toList(cacheCsvFile, ",");
    }

    public List<T> toList(String cacheCsvFile, String sep) {
        LineCache<T> cache = new LineCache<>(sep, type);
        return cache.get(cacheCsvFile, this::toList);
    }

    public <K, V> Map<K, V> groupBy(Function<T, K> keyMapper, Collector<T, ?, V> collector) {
        return supplier.get().collect(Collectors.groupingBy(keyMapper, collector));
    }
}
