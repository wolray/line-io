package com.github.wolray.line.io;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ray
 */
public class DataStream<T> {
    private Stream<T> stream;

    DataStream(Stream<T> stream) {
        this.stream = stream;
    }

    public DataStream<T> limit(int maxSize) {
        stream = stream.limit(maxSize);
        return this;
    }

    public DataStream<T> peek(Consumer<T> action) {
        stream = stream.peek(action);
        return this;
    }

    public DataStream<T> filter(Predicate<T> predicate) {
        stream = stream.filter(predicate);
        return this;
    }

    public void forEach(Consumer<T> action) {
        stream.forEach(action);
    }

    public List<T> toList() {
        return stream.collect(Collectors.toCollection(LinkedList::new));
    }

    public <K, V> Map<K, V> groupBy(Function<T, K> keyMapper,
        Collector<T, ?, V> collector) {
        return stream.collect(Collectors.groupingBy(keyMapper, collector));
    }
}
