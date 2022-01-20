package com.github.wolray.line.io;

import java.util.AbstractList;
import java.util.Iterator;

/**
 * @author wolray
 */
public class DataList<T> extends AbstractList<T> {
    private transient final Node<T> dummy = new Node<>();
    private transient Node<T> last = dummy;
    private transient int size = 0;

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

    private static class Node<T> {
        T t;
        Node<T> next;
    }
}
