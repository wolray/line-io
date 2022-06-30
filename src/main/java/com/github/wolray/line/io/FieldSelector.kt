package com.github.wolray.line.io

import java.lang.reflect.Field
import java.util.function.Predicate

/**
 * @author wolray
 */
internal class FieldSelector {
    var pojo = false
    var use: Array<String>? = null
    var omit: Array<String>? = null
    var useRegex: String? = null
    var omitRegex: String? = null

    fun toPredicate(): Predicate<Field> = with(NotEmpty) {
        use.ifNotEmpty {
            val set = toSet()
            return Predicate { set.contains(it.name) }
        }
        omit.ifNotEmpty {
            val set = toSet()
            return Predicate { !set.contains(it.name) }
        }
        useRegex.ifNotEmpty {
            val regex = toRegex()
            return Predicate { it.name.matches(regex) }
        }
        omitRegex.ifNotEmpty {
            val regex = toRegex()
            return Predicate { !it.name.matches(regex) }
        }
        Predicate { true }
    }

    companion object {
        @JvmStatic
        fun of(fields: Fields?) = FieldSelector().useWithKt(fields) {
            pojo = it.pojo
            use = it.use
            omit = it.omit
            useRegex = it.useRegex
            omitRegex = it.omitRegex
        }

        @JvmStatic
        fun toPredicate(fields: Fields?): Predicate<Field> {
            return fields?.let { of(it).toPredicate() } ?: Predicate { true }
        }
    }
}
