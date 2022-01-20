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
        Node<T>().also {
            it.t = element
            last.next = it
            last = it
            size++
        }
        return true
    }

    override fun get(index: Int): T {
        throw UnsupportedOperationException()
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

    private class Node<T> {
        var t: T? = null
        var next: Node<T>? = null
    }
}
