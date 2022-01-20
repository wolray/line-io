package com.github.wolray.line.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author ray
 */
public class IteratorHelper {
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

    public static Iterator<String> toIterator(BufferedReader reader) {
        return new Iterator<String>() {
            String nextLine = null;

            @Override
            public boolean hasNext() {
                if (nextLine != null) {
                    return true;
                } else {
                    try {
                        nextLine = reader.readLine();
                        return (nextLine != null);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }

            @Override
            public String next() {
                if (nextLine != null || hasNext()) {
                    String line = nextLine;
                    nextLine = null;
                    return line;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
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
