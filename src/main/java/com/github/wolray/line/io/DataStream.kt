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
class DataStream<T>(private var supplier: Supplier<Stream<T>>) :
    Chainable<DataStream<T>>,
    Cacheable<T, DataStream<T>>() {
    override val self: DataStream<T> = this
    private var ts: List<T>? = null

    private constructor(ts: List<T>) : this(ts::stream) {
        this.ts = ts
    }

    fun isReusable() = ts != null

    fun reuse() = apply {
        if (!isReusable()) {
            ts = supplier.get().collect(Collectors.toCollection { DataList<T>() })
            supplier = Supplier { ts!!.stream() }
        }
    }

    override fun from(session: LineReader<*, *, T>.Session) = session.stream()

    override fun after() = this

    fun limit(maxSize: Int): DataStream<T> = mapKt { limit(maxSize.toLong()) }

    fun peek(action: Consumer<T>): DataStream<T> = mapKt { peek(action) }

    fun filter(predicate: Predicate<T>): DataStream<T> = mapKt { filter(predicate) }

    fun <E> map(mapper: Function<T, E>): DataStream<E> = mapKt { map(mapper) }

    fun <E> mapBy(function: Function<Stream<T>, Stream<E>>): DataStream<E> {
        return DataStream { function.apply(supplier.get()) }
    }

    fun <E> mapKt(block: Stream<T>.() -> Stream<E>): DataStream<E> {
        return DataStream { supplier.get().block() }
    }

    fun forEach(action: Consumer<T>) {
        ts?.forEach(action) ?: supplier.get().forEach(action)
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
        return supplier.get().collect(Collectors.groupingBy(keyMapper, collector))
    }

    companion object {
        @JvmStatic
        fun <T> of(supplier: Supplier<Stream<T>>) = DataStream(supplier)

        @JvmStatic
        fun <T> of(ts: Iterable<T>): DataStream<T> = when (ts) {
            is List -> DataStream(ts)
            is Collection -> DataStream { ts.stream() }
            else -> DataStream { ts.asSequence().asStream() }
        }

        @JvmStatic
        fun <T> empty(): DataStream<T> = DataStream { Stream.empty() }
    }
}
