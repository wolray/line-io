package com.github.wolray.line.io

import com.github.wolray.line.io.EmptyScope.ifNotEmpty
import java.lang.reflect.Field

/**
 * @author wolray
 */
class FieldSelector {
    var pojo = false
    var use: Array<String>? = null
    var omit: Array<String>? = null
    var useRegex: String? = null
    var omitRegex: String? = null

    fun toTest(): (Field) -> Boolean {
        use.ifNotEmpty {
            val set = toSet()
            return { set.contains(it.name) }
        }
        omit.ifNotEmpty {
            val set = toSet()
            return { !set.contains(it.name) }
        }
        useRegex.ifNotEmpty {
            val regex = toRegex()
            return { it.name.matches(regex) }
        }
        omitRegex.ifNotEmpty {
            val regex = toRegex()
            return { !it.name.matches(regex) }
        }
        return { true }
    }

    companion object {
        @JvmStatic
        fun of(fields: Fields?) = FieldSelector().apply {
            fields?.also {
                pojo = it.pojo
                use = it.use
                omit = it.omit
                useRegex = it.useRegex
                omitRegex = it.omitRegex
            }
        }
    }
}
