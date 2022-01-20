package com.github.wolray.line.io

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * @author wolray
 */
class TypeValues<T> @JvmOverloads constructor(
    val type: Class<T>, fields: Fields? = type.getAnnotation(Fields::class.java)
) {
    val values: List<Field>

    init {
        val selector = makeSelector(fields)
        values = getFields(type, fields)
            .filter { checkModifier(it.modifiers) && selector.invoke(it) }
            .toList()
    }

    fun toAttrs(): Array<Attr> {
        return values.map { Attr(it) }.toTypedArray()
    }

    private fun getFields(type: Class<T>, fields: Fields?): Sequence<Field> {
        return if (fields != null && fields.pojo) {
            type.declaredFields.asSequence()
                .filter { Modifier.isPrivate(it.modifiers) }
                .onEach { it.isAccessible = true }
        } else {
            type.fields.asSequence()
        }
    }

    class SimpleMethod(val method: Method, val paraType: Class<*>, val returnType: Class<*>)

    class Attr internal constructor(val field: Field) {
        internal var mapper: ((String) -> String)? = null
        internal var parser: ((String) -> Any?)? = null
        internal var function: ((Any?) -> Any?)? = null
        internal var formatter: ((Any?) -> String)? = null

        internal fun composeMapper() {
            mapper?.run {
                val old = parser
                parser = { old!!.invoke(invoke(it)) }
            }
        }

        fun parse(s: String): Any? {
            return if (s.isNotEmpty()) parser!!.invoke(s) else null
        }

        fun convert(o: Any?): Any? {
            return if (o != null) function!!.invoke(o) else null
        }

        operator fun set(t: Any?, o: Any?) {
            try {
                field[t] = o
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            }
        }

        fun format(o: Any?): String {
            return o?.let { formatter!!.invoke(it) } ?: ""
        }

        operator fun get(t: Any?): Any? {
            return try {
                field[t]
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            }
        }
    }

    companion object {
        private fun checkModifier(modifier: Int): Boolean {
            return modifier and (Modifier.STATIC or Modifier.FINAL or Modifier.TRANSIENT) == 0
        }

        internal operator fun invoke(method: Method, o: Any?): Any? {
            return try {
                method.invoke(null, o)
            } catch (e: IllegalAccessException) {
                null
            } catch (e: InvocationTargetException) {
                null
            }
        }

        internal fun makeSelector(fields: Fields?): (Field) -> Boolean {
            if (fields != null) {
                val use = fields.use
                if (use.isNotEmpty()) {
                    val set = HashSet(listOf(*use))
                    return { set.contains(it.name) }
                }
                val omit = fields.omit
                if (omit.isNotEmpty()) {
                    val set = HashSet(listOf(*omit))
                    return { !set.contains(it.name) }
                }
                if (fields.regex.isNotEmpty()) {
                    val regex = fields.regex.toRegex()
                    return { it.name.matches(regex) }
                }
            }
            return { true }
        }

        internal fun processSimpleMethods(type: Class<*>, consumer: (SimpleMethod) -> Unit) {
            type.declaredMethods.asSequence()
                .filter { Modifier.isStatic(it.modifiers) }
                .forEach {
                    val parameterTypes = it.parameterTypes
                    val returnType = it.returnType
                    if (parameterTypes.size == 1 && returnType != Void.TYPE) {
                        consumer.invoke(SimpleMethod(it, parameterTypes[0], returnType))
                    }
                }
        }
    }
}
