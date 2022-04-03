package com.github.wolray.line.io.ext

/**
 * @author wolray
 */
inline fun <T> T.println(block: (T) -> String) = apply {
    println(block(this))
}

infix fun Boolean.onTrue(block: () -> Unit) = apply {
    if (this) block()
}

infix fun Boolean.onFalse(block: () -> Unit) = apply {
    if (not()) block()
}
