package com.github.wolray.line.io

import com.github.wolray.line.io.LineReader.Companion.byJson
import com.github.wolray.line.io.LineReader.Companion.toInputStream
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.UnaryOperator
import java.util.stream.Collector
import java.util.stream.Collectors

/**
 * @author wolray
 */
class DataStream<T> {
    private var ts: List<T>? = null
    private var supplier: (() -> Sequence<T>)? = null

    internal constructor(iterable: Iterable<T>) {
        if (iterable is List<*>) {
            setList(iterable as List<T>)
        } else {
            supplier = { iterable.iterator().asSequence() }
        }
    }

    internal constructor(supplier: () -> Sequence<T>) {
        this.supplier = supplier
    }

    private fun setList(list: List<T>) {
        ts = list
        supplier = { list.iterator().asSequence() }
    }

    fun isReusable(): Boolean {
        return ts != null
    }

    fun reuse(): DataStream<T> {
        if (!isReusable()) {
            setList(supplier!!.invoke().toCollection(DataList()))
        }
        return this
    }

    private fun mod(op: (Sequence<T>) -> Sequence<T>): DataStream<T> {
        val old = supplier
        val next = { op.invoke(old!!.invoke()) }
        return if (isReusable()) {
            DataStream(next)
        } else {
            supplier = next
            this
        }
    }

    fun peek(action: Consumer<T>): DataStream<T> {
        val consumer = action::accept
        return mod { s -> s.onEach(consumer) }
    }

    fun parallelPeek(action: Consumer<T>): DataStream<T> {
        toList().parallelStream().forEach(action)
        return this
    }

    fun limit(maxSize: Int): DataStream<T> {
        return mod { s -> s.take(maxSize) }
    }

    fun filter(predicate: Predicate<T>): DataStream<T> {
        val p = predicate::test
        return mod { s -> s.filter(p) }
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

    private fun cacheFile(
        file: String, suffix: String,
        reader: () -> LineReader.Text<T>,
        writer: () -> LineWriter<T>
    ): DataStream<T> {
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
                writer.invoke().write(ts, f)
            }
        })
    }

    @JvmOverloads
    fun cacheCsv(file: String, type: Class<T>, sep: String = ","): DataStream<T> {
        val mapper = DataMapper(TypeValues(type), sep)
        return cacheCsv(file, mapper)
    }

    fun cacheCsv(file: String, mapper: DataMapper<T>): DataStream<T> {
        return cacheFile(file, ".csv", { mapper.toReader() }, { mapper.toWriter() })
    }

    fun cacheJson(file: String, type: Class<T>): DataStream<T> {
        return cacheFile(file, ".txt", { byJson(type) }, { LineWriter.byJson() })
    }

    fun <E> map(mapper: Function<T, E>): DataStream<E> {
        val old = supplier
        return DataStream { old!!.invoke().map { mapper.apply(it) } }
    }

    fun forEach(action: Consumer<T>) {
        if (isReusable()) {
            ts!!.forEach(action)
        } else {
            supplier!!.invoke().forEach(action::accept)
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
        val set: MutableSet<K> = HashSet(ts.size)
        ts.forEach { set.add(mapper.apply(it)) }
        return set
    }

    fun <K, V> toMap(keyMapper: Function<T, K>, valueMapper: Function<T, V>): Map<K, V> {
        val ts = toList()
        val map: MutableMap<K, V> = HashMap(ts.size)
        ts.forEach { map[keyMapper.apply(it)] = valueMapper.apply(it) }
        return map
    }

    fun <K, V> groupBy(keyMapper: Function<T, K>, collector: Collector<T, *, V>): Map<K, V> {
        return toList().stream().collect(Collectors.groupingBy(keyMapper, collector))
    }

    interface Cache<T> {
        fun exists(): Boolean
        fun read(): DataStream<T>
        fun write(ts: List<T>)
    }

    companion object {
        @JvmStatic
        fun <T> of(ts: Iterable<T>): DataStream<T> {
            return DataStream(ts)
        }

        @JvmStatic
        fun <T> empty(): DataStream<T> {
            return of(emptyList())
        }
    }
}
