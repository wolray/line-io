package com.github.wolray.line.io

import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * @author wolray
 */
fun <T> Sequence<T>.enableCache() = object : Cacheable<T, Sequence<T>>() {
    override val self = this@enableCache

    override fun toList(): List<T> = self.toDataList()

    override fun from(session: LineReader<*, *, T>.Session): Sequence<T> {
        return session.sequence()
    }
}

@JvmOverloads
fun <T> Iterator<T>.toStream(size: Long? = null): Stream<T> {
    val c = Spliterator.ORDERED or Spliterator.NONNULL
    val spliterator = if (size == null) {
        Spliterators.spliteratorUnknownSize(this, c)
    } else {
        Spliterators.spliterator(this, size, c)
    }
    return StreamSupport.stream(spliterator, false)
}

fun <T> Sequence<T>.toDataList(): List<T> = toCollection(DataList())

inline fun <T, K, V> Grouping<T, K>.toSet(vMapper: (T) -> V): MutableMap<K, MutableSet<V>> {
    return toSet { acc, t -> acc.add(vMapper.invoke(t)) }
}

inline fun <T, K, V> Grouping<T, K>.toSet(appender: (MutableSet<V>, T) -> Unit): MutableMap<K, MutableSet<V>> {
    return foldTo(HashMap(), HashSet()) { acc, t ->
        appender.invoke(acc, t)
        acc
    }
}

inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T {
    return if (condition) apply(block) else this
}

inline fun <T, E> T.applyIfExists(e: E?, block: T.(E) -> Unit): T {
    return if (e != null) apply { block(e) } else this
}

inline fun <T> T.unaryIf(condition: Boolean, op: T.() -> T): T {
    return if (condition) op.invoke(this) else this
}

inline fun <T, E> T.unaryIfExists(e: E?, op: T.(E) -> T): T {
    return if (e != null) op.invoke(this, e) else this
}
