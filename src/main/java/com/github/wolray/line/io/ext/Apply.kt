package com.github.wolray.line.io.ext

/**
 * @author wolray
 */
inline fun <T> T.println(block: (T) -> String) = apply {
    println(block(this))
}

inline fun <T, E> T.useWith(e: E?, block: T.(E) -> Unit) = apply {
    if (e != null) block(e)
}

infix fun Boolean.onTrue(block: () -> Unit) = apply {
    if (this) block()
}

infix fun Boolean.onFalse(block: () -> Unit) = apply {
    if (not()) block()
}
