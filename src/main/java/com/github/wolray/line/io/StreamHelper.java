package com.github.wolray.line.io;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author ray
 */
public class StreamHelper {
    public static <T> Stream<T> toStream(Iterator<T> iterator, Long size) {
        int c = Spliterator.ORDERED | Spliterator.NONNULL;
        Spliterator<T> spliterator;
        if (size == null) {
            spliterator = Spliterators.spliteratorUnknownSize(iterator, c);
        } else {
            spliterator = Spliterators.spliterator(iterator, size, c);
        }
        return StreamSupport.stream(spliterator, false);
    }

    public static <V, T> Stream<T> consumeFirst(Stream<V> stream,
        Consumer<V> head, Function<V, T> rest) {
        Function<V, T> f = v -> {
            head.accept(v);
            return null;
        };
        return stream.map(twoStage(f, rest)).skip(1);
    }

    public static <V> Consumer<V> twoStage(Consumer<V> head, Consumer<V> rest) {
        return new Consumer<V>() {
            boolean isRest = false;

            @Override
            public void accept(V v) {
                if (isRest) {
                    rest.accept(v);
                } else {
                    isRest = true;
                    head.accept(v);
                }
            }
        };
    }

    public static <V, T> Function<V, T> twoStage(Function<V, T> head, Function<V, T> rest) {
        return new Function<V, T>() {
            boolean isRest = false;

            @Override
            public T apply(V v) {
                if (isRest) {
                    return rest.apply(v);
                } else {
                    isRest = true;
                    return head.apply(v);
                }
            }
        };
    }
}
