package com.github.wolray.line.io.ext

import com.github.wolray.line.io.Cacheable
import com.github.wolray.line.io.DataList
import com.github.wolray.line.io.LineReader

/**
 * @author wolray
 */
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
