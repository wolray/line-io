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

    DataStream<T> mod(UnaryOperator<Stream<T>> op) {
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

    public DataStream.Cache<T> cache(String csvFile) {
        return cache(csvFile, ",");
    }

    public DataStream.Cache<T> cache(String csvFile, String sep) {
        LineCache<T> cache = new LineCache<>(sep, type);
        List<T> ts = cache.get(csvFile, this::toList);
        return new DataStream.Cache<>(ts);
    }

    public void forEach(Consumer<T> action) {
        supplier.get().forEach(action);
    }

    public List<T> toList() {
        return supplier.get().collect(Collectors.toCollection(DataList::new));
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

    public static class Cache<T> extends DataStream<T> {
        private final List<T> ts;

        private Cache(List<T> ts) {
            super(null, ts::stream);
            this.ts = ts;
        }

        @Override
        DataStream<T> mod(UnaryOperator<Stream<T>> op) {
            throw new IllegalStateException("cannot modify cached stream");
        }

        @Override
        public Cache<T> cache(String csvFile, String sep) {
            throw new IllegalStateException("stream is already cached");
        }

        @Override
        public void forEach(Consumer<T> action) {
            ts.forEach(action);
        }

        @Override
        public List<T> toList() {
            return ts;
        }
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
