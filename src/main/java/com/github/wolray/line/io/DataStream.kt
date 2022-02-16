package com.github.wolray.line.io

import java.util.function.*
import java.util.function.Function
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * @author wolray
 */
class DataStream<T> {
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

    fun consumeIf(condition: Boolean, consumer: Consumer<DataStream<T>>): DataStream<T> {
        if (condition) {
            consumer.accept(this)
            if (isReusable().not()) {
                return empty()
            }
        }
        return this
    }

    fun operateIf(condition: Boolean, op: UnaryOperator<DataStream<T>>): DataStream<T> {
        return if (condition) op.apply(this) else this
    }

    fun cacheBy(cache: Cache<T>): DataStream<T> {
        return if (cache.exists()) {
            cache.read()
        } else {
            val ts = toList()
            if (ts.isNotEmpty()) {
                cache.write(ts)
            }
            this
        }
    }

    private fun cacheFile(
        file: String, suffix: String,
        reader: () -> LineReader.Text<T>,
        writer: () -> LineWriter<T>
    ): DataStream<T> {
        val f = if (file.endsWith(suffix)) file else file + suffix
        val input = LineReader.toInputStream(f)
        return cacheBy(object : Cache<T> {
            override fun exists() = input != null
            override fun read() = reader.invoke().read(input).stream()
            override fun write(ts: List<T>) = writer.invoke().write(f).with(ts)
        })
    }

    @JvmOverloads
    fun cacheCsv(file: String, type: Class<T>, sep: String = ",") =
        cacheCsv(file, DataMapper.simple(type, sep))

    fun cacheCsv(file: String, mapper: DataMapper<T>) =
        cacheFile(file, ".csv", mapper::toReader, mapper::toWriter)

    fun cacheJson(file: String, type: Class<T>) =
        cacheFile(file, ".txt", { LineReader.byJson(type) }, { LineWriter.byJson() })

    fun <E> map(mapper: Function<T, E>): DataStream<E> {
        val old = supplier
        return DataStream { old!!.invoke().map(mapper) }
    }

    fun forEach(action: Consumer<T>) {
        if (isReusable()) {
            ts!!.forEach(action)
        } else {
            supplier!!.invoke().forEach(action)
        }
    }

    fun parallelFor(action: Consumer<T>) {
        toList().parallelStream().forEach(action)
    }

    fun toList(): List<T> {
        reuse()
        return ts!!
    }

    fun toArrayList() = ArrayList(toList())

    fun <K> toSet(mapper: Function<T, K>): Set<K> {
        val ts = toList()
        return HashSet<K>(ts.size).apply {
            ts.forEach { add(mapper.apply(it)) }
        }
    }

    fun <K, V> toMap(keyMapper: Function<T, K>, valueMapper: Function<T, V>): Map<K, V> {
        val ts = toList()
        return HashMap<K, V>(ts.size).apply {
            ts.forEach { this[keyMapper.apply(it)] = valueMapper.apply(it) }
        }
    }

    fun <K, V> groupBy(keyMapper: Function<T, K>, collector: Collector<T, *, V>): Map<K, V> {
        return supplier!!.invoke().collect(Collectors.groupingBy(keyMapper, collector))
    }

    interface Cache<T> {
        fun exists(): Boolean
        fun read(): DataStream<T>
        fun write(ts: List<T>)
    }

    companion object {
        @JvmStatic
        fun <T> of(ts: Collection<T>) =
            if (ts is List<*>) {
                DataStream(ts as List<T>)
            } else {
                DataStream { ts.stream() }
            }

        @JvmStatic
        fun <T> of(supplier: Supplier<Stream<T>>) = DataStream { supplier.get() }

        @JvmStatic
        fun <T> empty(): DataStream<T> = DataStream { Stream.empty() }
    }
}
