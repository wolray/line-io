package com.github.wolray.line.io

import com.github.wolray.line.io.LineReader.Companion.byJson
import com.github.wolray.line.io.LineReader.Companion.toInputStream
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
    private var supplier: Supplier<Stream<T>>? = null

    private constructor(ts: List<T>) {
        setList(ts)
    }

    private constructor(supplier: Supplier<Stream<T>>) {
        this.supplier = supplier
    }

    private fun setList(list: List<T>) {
        ts = list
        supplier = Supplier { list.stream() }
    }

    fun isReusable(): Boolean {
        return ts != null
    }

    fun reuse(): DataStream<T> {
        if (!isReusable()) {
            setList(supplier!!.get().collect(Collectors.toCollection { DataList<T>() }))
        }
        return this
    }

    private fun mod(op: UnaryOperator<Stream<T>>): DataStream<T> {
        val old = supplier
        val next = Supplier { op.apply(old!!.get()) }
        return if (isReusable()) {
            of(next)
        } else {
            supplier = next
            this
        }
    }

    fun peek(action: Consumer<T>): DataStream<T> {
        return mod { s: Stream<T> -> s.peek(action) }
    }

    fun parallelPeek(action: Consumer<T>): DataStream<T> {
        toList().parallelStream().forEach(action)
        return this
    }

    fun limit(maxSize: Int): DataStream<T> {
        return mod { s: Stream<T> -> s.limit(maxSize.toLong()) }
    }

    fun filter(predicate: Predicate<T>): DataStream<T> {
        return mod { s: Stream<T> -> s.filter(predicate) }
    }

    fun consumeIf(condition: Boolean, consumer: Consumer<DataStream<T>>): DataStream<T> {
        if (condition) {
            consumer.accept(this)
            return if (isReusable()) this else empty()
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

    private fun cacheFile(file: String, suffix: String,
                          reader: () -> LineReader.Text<T>,
                          writer: () -> LineWriter<T>): DataStream<T> {
        val f = if (file.endsWith(suffix)) file else file + suffix
        val input = toInputStream(f)
        return cacheBy(object : Cache<T> {
            override fun exists(): Boolean {
                return input != null
            }

            override fun read(): DataStream<T> {
                return reader.invoke().read(input!!).stream()
            }

            override fun write(ts: List<T>) {
                writer.invoke().writeAsync(ts, f)
            }
        })
    }

    @JvmOverloads
    fun cacheCsv(file: String, type: Class<T>, sep: String = ","): DataStream<T> {
        val mapper = DataMapper(TypeValues(type), sep)
        return cacheCsv(file, mapper)
    }

    fun cacheCsv(file: String, mapper: DataMapper<T>): DataStream<T> {
        return cacheFile(file, ".csv", mapper::toReader, mapper::toWriter)
    }

    fun cacheJson(file: String, type: Class<T>): DataStream<T> {
        return cacheFile(file, ".txt", { byJson(type) }, { LineWriter.byJson() })
    }

    fun <E> map(mapper: Function<T, E>): DataStream<E> {
        val old = supplier
        return of { old!!.get().map(mapper) }
    }

    fun forEach(action: Consumer<T>) {
        if (isReusable()) {
            ts!!.forEach(action)
        } else {
            supplier!!.get().forEach(action)
        }
    }

    fun toList(): List<T> {
        reuse()
        return ts ?: throw AssertionError()
    }

    fun toArrayList(): List<T> {
        return ArrayList(toList())
    }

    fun <K> toSet(mapper: Function<T, K>): Set<K> {
        val ts = toList()
        return ts.map(mapper::apply).toCollection(HashSet(ts.size))
    }

    fun <K, V> toMap(keyMapper: Function<T, K>, valueMapper: Function<T, V>): Map<K, V> {
        val ts = toList()
        val map: MutableMap<K, V> = HashMap(ts.size)
        ts.forEach { map[keyMapper.apply(it)] = valueMapper.apply(it) }
        return map
    }

    fun <K, V> groupBy(keyMapper: Function<T, K>, collector: Collector<T, *, V>): Map<K, V> {
        return supplier!!.get().collect(Collectors.groupingBy(keyMapper, collector))
    }

    interface Cache<T> {
        fun exists(): Boolean
        fun read(): DataStream<T>
        fun write(ts: List<T>)
    }

    companion object {
        @JvmStatic
        fun <T> of(ts: Collection<T>): DataStream<T> {
            return if (ts is List<*>) {
                DataStream(ts as List<T>)
            } else {
                of { ts.stream() }
            }
        }

        @JvmStatic
        fun <T> of(supplier: Supplier<Stream<T>>): DataStream<T> {
            return DataStream(supplier)
        }

        @JvmStatic
        fun <T> empty(): DataStream<T> {
            return of { Stream.empty() }
        }
    }
}
