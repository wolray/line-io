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
    final Class<T> type;
    Supplier<Stream<T>> supplier;

    DataStream(Class<T> type, Supplier<Stream<T>> supplier) {
        this.type = type;
        this.supplier = supplier;
    }

    public static <T> DataStream<T> of(List<T> ts) {
        return of(ts, null);
    }

    public static <T> DataStream<T> of(List<T> ts, Class<T> type) {
        return new Cache<>(type, ts);
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

    public DataStream<T> cache() {
        return new Cache<>(type, toList());
    }

    public DataStream<T> csvCache(String file) {
        return csvCache(file, ",");
    }

    public DataStream<T> csvCache(String file, String sep) {
        if (type == null) {
            throw new IllegalStateException("unspecified type");
        }
        String suffix = ".csv";
        if (!file.endsWith(suffix)) {
            file = file + suffix;
        }
        InputStream is = LineReader.toInputStream(file);
        LineReader.Csv<T> reader = LineReader.byCsv(sep, type);
        if (is != null) {
            return reader.read(is);
        }
        List<T> list = toList();
        LineWriter<T> writer = LineWriter.byCsv(sep, type);
        writer.writeAsync(list, file);
        return new Cache<>(type, list);
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

        private Cache(Class<T> type, List<T> ts) {
            super(type, ts::stream);
            this.ts = ts;
        }

        @Override
        DataStream<T> mod(UnaryOperator<Stream<T>> op) {
            return new DataStream<>(type, () -> op.apply(supplier.get()));
        }

        @Override
        public DataStream<T> cache() {
            return this;
        }

        @Override
        public DataStream<T> csvCache(String file, String sep) {
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
