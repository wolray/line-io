package com.github.wolray.line.io

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.util.function.Supplier

/**
 * @author wolray
 */
object MethodScope {
    inline fun <reified T : Annotation> AnnotatedElement.annotation(): T? =
        getAnnotation(T::class.java)

    fun <A, B> Method.asMapper(): (A) -> B? = {
        isAccessible = true
        call(it)
    }

    fun <A, B> Method.asMapper(default: B): (A) -> B = {
        isAccessible = true
        call(it) ?: default
    }

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

object TextScope {
    fun Text.toSequence() = BufferedReader(InputStreamReader(get())).lineSequence()
}

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
    inline fun <reified T : Any> Class<*>.isType(checkPrimitive: Boolean = false): Boolean =
        this == T::class.javaObjectType || checkPrimitive && this == T::class.javaPrimitiveType
}