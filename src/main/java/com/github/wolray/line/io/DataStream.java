package com.github.wolray.line.io;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
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

    public DataStream<T> limit(int maxSize) {
        Supplier<Stream<T>> old = supplier;
        supplier = () -> old.get().limit(maxSize);
        return this;
    }

    public DataStream<T> peek(Consumer<T> action) {
        Supplier<Stream<T>> old = supplier;
        supplier = () -> old.get().peek(action);
        return this;
    }

    public DataStream<T> filter(Predicate<T> predicate) {
        Supplier<Stream<T>> old = supplier;
        supplier = () -> old.get().filter(predicate);
        return this;
    }

    public void forEach(Consumer<T> action) {
        supplier.get().forEach(action);
    }

    public List<T> toList() {
        return supplier.get().collect(Collectors.toCollection(LinkedList::new));
    }

    public List<T> toListWithCsvCache(String sep, String file) {
        LineCache<T> cache = new LineCache<>(sep, type);
        return cache.get(file, this::toList);
    }

    public <K, V> Map<K, V> groupBy(Function<T, K> keyMapper, Collector<T, ?, V> collector) {
        return supplier.get().collect(Collectors.groupingBy(keyMapper, collector));
    }
}
