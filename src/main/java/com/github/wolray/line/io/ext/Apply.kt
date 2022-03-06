package com.github.wolray.line.io.ext

/**
 * @author wolray
 */
inline fun <T> T.useIf(condition: Boolean, block: T.() -> Unit) = apply {
    if (condition) block()
}

inline fun <T, E> T.useWith(e: E?, block: T.(E) -> Unit) = apply {
    if (e != null) block(e)
}

inline fun <T> T.println(block: (T) -> String) = apply {
    println(block(this))
}

infix fun Boolean.onTrue(block: () -> Unit) = apply {
    if (this) block()
}

infix fun Boolean.onFalse(block: () -> Unit) = apply {
    if (not()) block()
}
