package com.github.wolray.line.io

import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * @author wolray
 */
class MutableLazy<T>(private val supplier: Supplier<T>) {
    private var cache: T? = null

    fun get() = cache ?: supplier.get().also { set(it) }

    fun set(t: T) {
        cache = t
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

fun <T> Sequence<T>.enableCache() = object : Cacheable<T, Sequence<T>>() {
    private var seq = this@enableCache

    override fun from(session: LineReader<*, *, T>.Session) = session.sequence()

    override fun toList() = seq.toDataList().also { seq = CachedSequence(it) }

    override fun after() = seq
}

class CachedSequence<T>(internal val ts: List<T>) : Sequence<T> by ts.asSequence()

fun <T> Sequence<T>.toDataList(): List<T> = when (this) {
    is CachedSequence<T> -> ts
    else -> toCollection(DataList())
}

inline fun <T, K, V> Grouping<T, K>.toSet(vMapper: (T) -> V): MutableMap<K, MutableSet<V>> {
    return toSetBy { acc, t -> acc.add(vMapper.invoke(t)) }
}

inline fun <T, K, V> Grouping<T, K>.toSetBy(appender: (MutableSet<V>, T) -> Unit):
    MutableMap<K, MutableSet<V>> {
    return foldBy(HashSet(), appender)
}

inline fun <T, K, V> Grouping<T, K>.foldBy(init: V, appender: (V, T) -> Unit):
    MutableMap<K, V> {
    return foldTo(HashMap(), init) { acc, t ->
        appender.invoke(acc, t)
        acc
    }
}

fun <T> List<T>.asMutable() = this as MutableList

fun <K, V> Map<K, V>.asMutable() = this as MutableMap

fun <T> Set<T>.asMutable() = this as MutableSet

inline fun <T> T.useIf(condition: Boolean, block: T.() -> Unit) = apply {
    if (condition) block()
}

inline fun <T, E> T.useWith(e: E?, block: T.(E) -> Unit) = apply {
    if (e != null) block(e)
}

inline fun <T> T.operateIf(condition: Boolean, block: T.() -> T): T {
    return if (condition) block() else this
}

inline fun <T, E> T.operateWith(e: E?, block: T.(E) -> T): T {
    return if (e != null) block(e) else this
}

inline fun <T> getInMillis(block: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    val t = block()
    return t to System.currentTimeMillis() - start
}

inline fun <T> T.println(block: (T) -> String) = apply {
    println(block(this))
}