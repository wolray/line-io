package com.github.wolray.line.io;

import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

/**
 * @author wolray
 */
interface Chainable<T, B> {
    val self: T
    val box: B

    fun use(consumer: Consumer<T>): B {
        consumer.accept(self)
        return box
    }

    fun useIf(condition: Boolean, consumer: Consumer<T>): B {
        if (condition) {
            consumer.accept(self)
        }
        return box
    }

    fun <E> useWith(e: E?, consumer: BiConsumer<T, E>): B {
        if (e != null) {
            consumer.accept(self, e)
        }
        return box
    }

    fun chain(function: Function<T, B>): B {
        return function.apply(self)
    }

    fun chainIf(condition: Boolean, function: Function<T, B>): B {
        return if (condition) function.apply(self) else box
    }

    fun <E> chainWith(e: E?, function: BiFunction<T, E, B>): B {
        return if (e != null) function.apply(self, e) else box
    }

    companion object {
        @JvmStatic
        fun <T> box(t: T) = ChainBox(t)
    }
}

class ChainBox<T>(t: T) : Chainable<T, ChainBox<T>> {
    override val self: T = t
    override val box: ChainBox<T> = this
}

interface SelfChainable<T> : Chainable<T, T> {
    override val box: T get() = self
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

