package com.github.wolray.line.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author wolray
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
}
