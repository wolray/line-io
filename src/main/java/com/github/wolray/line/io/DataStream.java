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
    private Supplier<Stream<T>> streamSupplier;

    DataStream(Supplier<Stream<T>> streamSupplier) {
        this.streamSupplier = streamSupplier;
    }

    public DataStream<T> limit(int maxSize) {
        streamSupplier = () -> streamSupplier.get().limit(maxSize);
        return this;
    }

    public DataStream<T> peek(Consumer<T> action) {
        streamSupplier = () -> streamSupplier.get().peek(action);
        return this;
    }

    public DataStream<T> filter(Predicate<T> predicate) {
        streamSupplier = () -> streamSupplier.get().filter(predicate);
        return this;
    }

    private Stream<T> get() {
        return streamSupplier.get();
    }

    public void forEach(Consumer<T> action) {
        get().forEach(action);
    }

    public List<T> toList() {
        return get().collect(Collectors.toCollection(LinkedList::new));
    }

    public <K, V> Map<K, V> groupBy(Function<T, K> keyMapper, Collector<T, ?, V> collector) {
        return get().collect(Collectors.groupingBy(keyMapper, collector));
    }
}
