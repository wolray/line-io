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

    fun toPredicate(): Predicate<Field> {
        if (use.isNullOrEmpty().not()) {
            val set = use!!.toSet()
            return Predicate { set.contains(it.name) }
        }
        if (omit.isNullOrEmpty().not()) {
            val set = omit!!.toSet()
            return Predicate { !set.contains(it.name) }
        }
        if (useRegex.isNullOrEmpty().not()) {
            val regex = useRegex!!.toRegex()
            return Predicate { it.name.matches(regex) }
        }
        if (omitRegex.isNullOrEmpty().not()) {
            val regex = omitRegex!!.toRegex()
            return Predicate { !it.name.matches(regex) }
        }
        return Predicate { true }
    }

    companion object {
        @JvmStatic
        fun of(fields: Fields?) = FieldSelector().useWith(fields) {
            pojo = it.pojo
            use = it.use
            omit = it.omit
            useRegex = it.useRegex
            omitRegex = it.omitRegex
        }

        @JvmStatic
        fun toPredicate(fields: Fields?): Predicate<Field> {
            return if (fields == null) Predicate { true } else of(fields).toPredicate()
        }
    }
}
