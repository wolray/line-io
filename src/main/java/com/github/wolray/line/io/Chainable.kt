package com.github.wolray.line.io;

import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.UnaryOperator

/**
 * @author wolray
 */
interface Chainable<T> {
    val self: T

    fun use(consumer: Consumer<T>): T {
        consumer.accept(self)
        return self
    }

    fun useIf(condition: Boolean, consumer: Consumer<T>): T {
        if (condition) {
            consumer.accept(self)
        }
        return self
    }

    fun <E> useWith(e: E?, consumer: BiConsumer<T, E>): T {
        if (e != null) {
            consumer.accept(self, e)
        }
        return self
    }

    fun chainIf(condition: Boolean, function: UnaryOperator<T>): T {
        return if (condition) function.apply(self) else self
    }

    fun <E> chainWith(e: E?, function: BiFunction<T, E, T>): T {
        return if (e != null) function.apply(self, e) else self
    }
}

inline fun <T, E> T.useWithKt(e: E?, block: T.(E) -> Unit) = apply {
    if (e != null) block(e)
}

inline fun <T> T.chainIfKt(condition: Boolean, block: T.() -> T): T {
    return if (condition) block() else this
}

inline fun <T, E> T.chainWithKt(e: E?, block: T.(E) -> T): T {
    return if (e != null) block(e) else this
}
