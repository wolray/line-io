package com.github.wolray.line.io.ext

/**
 * @author wolray
 */
@JvmOverloads
inline fun <T, K, V> Grouping<T, K>.toSet(
    des: MutableSet<V> = HashSet(),
    vMapper: (T) -> V
): MutableMap<K, MutableSet<V>> {
    return foldBy(des) { add(vMapper(it)) }
}

@JvmOverloads
inline fun <T, K1, K2, V> Grouping<T, K1>.associate(
    des: MutableMap<K2, V> = HashMap(),
    transformer: (T) -> Pair<K2, V>
): MutableMap<K1, MutableMap<K2, V>> {
    return foldBy(des) { plus(transformer(it)) }
}

@JvmOverloads
inline fun <T, K, V> Grouping<T, K>.associateBy(
    des: MutableMap<V, T> = HashMap(),
    keySelector: (T) -> V
): MutableMap<K, MutableMap<V, T>> {
    return foldBy(des) { put(keySelector(it), it) }
}

@JvmOverloads
inline fun <T, K, V> Grouping<T, K>.associateWith(
    des: MutableMap<T, V> = HashMap(),
    valueSelector: (T) -> V
): MutableMap<K, MutableMap<T, V>> {
    return foldBy(des) { put(it, valueSelector(it)) }
}

inline fun <T, K, V> Grouping<T, K>.foldBy(des: V, appender: V.(T) -> Unit):
    MutableMap<K, V> {
    return foldTo(HashMap(), des) { acc, t ->
        acc.appender(t)
        acc
    }
}

@JvmOverloads
inline fun <T, K> Grouping<T, K>.sumInt(init: Int = 0, block: (T) -> Int): Map<K, Int> {
    return fold(init) { acc, t -> acc + block(t) }
}

@JvmOverloads
inline fun <T, K> Grouping<T, K>.sumDouble(init: Double = 0.0, block: (T) -> Double): Map<K, Double> {
    return fold(init) { acc, t -> acc + block(t) }
}

@JvmOverloads
inline fun <T, K> Grouping<T, K>.sumLong(init: Long = 0, block: (T) -> Long): Map<K, Long> {
    return fold(init) { acc, t -> acc + block(t) }
}
