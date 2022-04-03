package com.github.wolray.line.io

import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.streams.asStream

/**
 * @author wolray
 */
class DataStream<T> : Cacheable<T, DataStream<T>>, Chainable<DataStream<T>> {
    override val self = this
    private var ts: List<T>? = null
    private var supplier: (() -> Stream<T>)? = null

    private constructor(ts: List<T>) {
        setList(ts)
    }

    private constructor(supplier: (() -> Stream<T>)) {
        this.supplier = supplier
    }

    private fun setList(list: List<T>) {
        ts = list
        supplier = { list.stream() }
    }

    fun isReusable() = ts != null

    fun reuse() = apply {
        if (!isReusable()) {
            setList(supplier!!.invoke().collect(Collectors.toCollection { DataList<T>() }))
        }
    }

    override fun from(session: LineReader<*, *, T>.Session) = session.stream()

    override fun after() = this

    private fun mod(op: (Stream<T>) -> Stream<T>): DataStream<T> {
        val old = supplier
        val next = { op.invoke(old!!.invoke()) }
        return if (isReusable()) {
            DataStream(next)
        } else {
            supplier = next
            this
        }
    }

    fun limit(maxSize: Int) = mod { it.limit(maxSize.toLong()) }

    fun peek(action: Consumer<T>) = mod { it.peek(action) }

    fun filter(predicate: Predicate<T>) = mod { it.filter(predicate) }

    fun <E> map(mapper: Function<T, E>): DataStream<E> {
        val old = supplier
        return DataStream { old!!.invoke().map(mapper) }
    }

    fun forEach(action: Consumer<T>) {
        ts?.forEach(action) ?: supplier!!.invoke().forEach(action)
    }

    fun parallelFor(action: Consumer<T>) {
        toList().parallelStream().forEach(action)
    }

    override fun toList(): List<T> {
        reuse()
        return ts!!
    }

    fun toArrayList() = ArrayList(toList())

    fun <K> toSet(mapper: Function<T, K>): Set<K> {
        return toList().run { mapTo(HashSet(size), mapper::apply) }
    }

    fun <K, V> toMap(keyMapper: Function<T, K>, valueMapper: Function<T, V>): Map<K, V> {
        return toList().associate { keyMapper.apply(it) to valueMapper.apply(it) }
    }

    fun <K, V> groupBy(keyMapper: Function<T, K>, collector: Collector<T, *, V>): Map<K, V> {
        return supplier!!.invoke().collect(Collectors.groupingBy(keyMapper, collector))
    }

    companion object {
        @JvmStatic
        fun <T> of(ts: Iterable<T>): DataStream<T> = when (ts) {
            is List -> DataStream(ts)
            is Collection -> DataStream { ts.stream() }
            else -> DataStream { ts.asSequence().asStream() }
        }

        @JvmStatic
        fun <T> of(supplier: Supplier<Stream<T>>) = DataStream { supplier.get() }

        @JvmStatic
        fun <T> empty(): DataStream<T> = DataStream { Stream.empty() }
    }
}
