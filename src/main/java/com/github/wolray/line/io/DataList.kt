package com.github.wolray.line.io

import java.util.*

/**
 * @author wolray
 */
class DataList<T> : AbstractList<T>() {
    @Transient
    private val dummy = Node<T>()

    @Transient
    private var last = dummy

    @Transient
    override var size = 0

    override fun add(element: T): Boolean {
        val it = Node<T>()
        it.t = element
        last.next = it
        last = it
        size++
        return true
    }

    override fun get(index: Int): T {
        var node = dummy
        if (index >= size) {
            throw IndexOutOfBoundsException("$index >= $size")
        }
        for (i in 0..index) {
            node = node.next!!
        }
        return node.t!!
    }

    override fun iterator(): MutableIterator<T> {
        return object : MutableIterator<T> {
            var node = dummy

            override fun hasNext(): Boolean {
                return node.next != null
            }

            override fun next(): T {
                node = node.next!!
                return node.t!!
            }

            override fun remove() {}
        }
    }

    override fun clear() {
        var node = dummy
        var next: Node<T>
        while (node.next != null) {
            next = node.next!!
            node.next = null
            node = next
        }
        size = 0
    }

    private class Node<T> {
        var t: T? = null
        var next: Node<T>? = null
    }
}
