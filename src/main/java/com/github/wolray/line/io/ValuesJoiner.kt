package com.github.wolray.line.io

import com.github.wolray.line.io.TypeValues.SimpleMethod
import java.lang.reflect.Field
import java.util.*

/**
 * @author wolray
 */
abstract class ValuesJoiner<T, E, V>(val typeValues: TypeValues<T>) {
    internal val attrs: Array<Attr<E>> = toAttrs()

    init {
        TypeValues.processSimpleMethods(typeValues.type, ::processMethod)
    }

    abstract fun toElement(o: Any?): E

    abstract fun processMethod(simpleMethod: SimpleMethod)

    class Attr<E>(val field: Field, var mapper: (Any?) -> E)

    private fun toAttrs(): Array<Attr<E>> {
        return typeValues.values.asSequence()
            .map { Attr(it, ::toElement) }
            .toList()
            .toTypedArray()
    }

    fun join(t: T, appender: Appender<E, V>): V {
        for (a in attrs) {
            appender.add(a.mapper(a.field[t]))
        }
        return appender.finalizer()
    }

    interface Appender<E, V> {
        fun add(e: E)
        fun finalizer(): V
    }

    class Csv<T>(typeValues: TypeValues<T>) :
        ValuesJoiner<T, String, String>(typeValues) {

        override fun toElement(o: Any?): String = o?.toString().orEmpty()

        fun joinFields(sep: String): String = join(sep) { it.field.name }

        fun join(sep: String, mapper: (Attr<String>) -> String): String {
            val joiner = StringJoiner(sep)
            for (a in attrs) {
                joiner.add(mapper(a))
            }
            return joiner.toString()
        }

        fun toFormatter(sep: String): (T) -> String = { join(sep) { a -> a.mapper(a.field[it]) } }

        override fun processMethod(simpleMethod: SimpleMethod) {
            val method = simpleMethod.method
            val paraType = simpleMethod.paraType
            if (paraType != String::class.java && simpleMethod.returnType == String::class.java) {
                method.isAccessible = true
                val mapper: (Any?) -> String = { TypeValues.call(method, it) ?: "" }
                val test = FieldSelector.of(method.getAnnotation(Fields::class.java)).toTest()
                attrs.asSequence()
                    .filter { test(it.field) && it.field.type == paraType }
                    .forEach { it.mapper = mapper }
            }
        }
    }
}