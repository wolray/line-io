package com.github.wolray.line.io;

import java.util.*;
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
        return supplier.get().collect(Collectors.toCollection(DataList::new));
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
    
    public static class DataList<T> extends AbstractList<T> {
        private final Node<T> dummy = new Node<>();
        private Node<T> last = dummy;
        private int size = 0;

        @Override
        public boolean add(T t) {
            Node<T> node = new Node<>();
            node.t = t;
            last.next = node;
            last = node;
            size++;
            return true;
        }

        @Override
        public T get(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                Node<T> node = dummy;

                @Override
                public boolean hasNext() {
                    return node.next != null;
                }

                @Override
                public T next() {
                    node = node.next;
                    return node.t;
                }
            };
        }
    }

    private static class Node<T> {
        T t;
        Node<T> next;
    }
}
