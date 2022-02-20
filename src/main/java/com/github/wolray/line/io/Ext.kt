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
    override val self = this@enableCache

    override fun toList(): List<T> = self.toDataList()

    override fun from(session: LineReader<*, *, T>.Session): Sequence<T> {
        return session.sequence()
    }
}

fun <T> Sequence<T>.toDataList(): List<T> = toCollection(DataList())

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

class Maybe<T>(private val t: T?, test: T.() -> Boolean) {
    private var b = t != null && t.test()

    fun call(block: T.() -> Unit) = apply {
        if (b) t!!.block()
    }

    fun orElse(block: () -> Unit) {
        if (!b) block()
    }

    fun <R> toRun(block: T.() -> R): R? {
        return if (b) t!!.block() else null
    }

    fun <R> toLet(block: (T) -> R): R? {
        return if (b) block(t!!) else null
    }
}

fun <T> T?.testIf(test: T.() -> Boolean) = Maybe(this, test)

inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T {
    return if (condition) apply(block) else this
}

inline fun <T, E> T.applyWith(e: E?, block: T.(E) -> Unit): T {
    return if (e != null) apply { block(e) } else this
}

inline fun <T> T.unaryIf(condition: Boolean, op: T.() -> T): T {
    return if (condition) op.invoke(this) else this
}

inline fun <T, E> T.unaryWith(e: E?, op: T.(E) -> T): T {
    return if (e != null) op.invoke(this, e) else this
}