package com.github.wolray.line.io

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Method
import java.util.function.Supplier

/**
 * @author wolray
 */
object MethodScope {
    fun <A, B> Method.asMapper(): (A) -> B? = { call(it) }
    fun <A, B> Method.asMapper(default: B): (A) -> B = { call(it) ?: default }

    @Suppress("UNCHECKED_CAST")
    private fun <A, B> Method.call(a: A?): B? {
        a ?: return null
        return try {
            invoke(null, a) as? B
        } catch (e: Throwable) {
            null
        }
    }
}

typealias Text = Supplier<InputStream>

fun Text.toSequence() = BufferedReader(InputStreamReader(get())).lineSequence()

object EmptyScope {
    inline fun <T> T?.ifNotEmpty(block: T.() -> Unit) {
        this?.takeIf {
            when (this) {
                is String -> isNotEmpty()
                is Array<*> -> isNotEmpty()
                is IntArray -> isNotEmpty()
                is Collection<*> -> isNotEmpty()
                is Map<*, *> -> isNotEmpty()
                is DoubleArray -> isNotEmpty()
                is LongArray -> isNotEmpty()
                else -> true
            }
        }?.run(block)
    }
}

object TypeScope {
    fun Class<*>.isString() = this == String::class.java

    fun Class<*>.isBool() =
        this == Boolean::class.javaObjectType || this == Boolean::class.javaPrimitiveType

    inline fun <reified T : Number> Class<*>.isNumber() =
        this == T::class.javaObjectType || this == T::class.javaPrimitiveType
}