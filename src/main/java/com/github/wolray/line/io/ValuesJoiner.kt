package com.github.wolray.line.io

import com.github.wolray.line.io.MethodScope.asMapper
import com.github.wolray.line.io.TypeScope.isString
import com.github.wolray.line.io.TypeValues.SimpleMethod
import java.lang.reflect.Field
import java.util.*

/**
 * @author wolray
 */
abstract class ValuesJoiner<T, E, V>(val typeValues: TypeValues<T>) {
    val attrs: Array<Attr<E>> = toAttrs()

    init {
        TypeValues.processSimpleMethods(typeValues.type, ::processMethod)
    }

    open fun processMethod(simpleMethod: SimpleMethod) {}

    abstract fun toElement(o: Any?): E

    class Attr<E>(val field: Field, var mapper: (Any?) -> E)

    private fun toAttrs(): Array<Attr<E>> {
        return typeValues.values.asSequence()
            .map { Attr(it, ::toElement) }
            .toList()
            .toTypedArray()
    }

    fun joinFields(sep: String): String = join(sep) { it.field.name }

    fun join(sep: String, mapper: (Attr<E>) -> String): String {
        val joiner = StringJoiner(sep)
        for (a in attrs) {
            joiner.add(mapper(a))
        }
        return joiner.toString()
    }

    fun join(t: T, appender: Appender<E, V>): V {
        for (a in attrs) {
            appender.add(a.mapper(a.field[t]))
        }
        return appender.finalizer()
    }

    fun toMapper(appender: () -> Appender<E, V>): (T) -> V = { join(it, appender()) }

    interface Appender<E, V> {
        fun add(e: E)
        fun finalizer(): V
    }

    class Csv<T>(typeValues: TypeValues<T>) :
        ValuesJoiner<T, String, String>(typeValues) {

        override fun toElement(o: Any?): String = o?.toString().orEmpty()

        fun toFormatter(sep: String): (T) -> String = { t -> join(sep) { it.mapper(it.field[t]) } }

        override fun processMethod(simpleMethod: SimpleMethod) {
            val method = simpleMethod.method
            val paraType = simpleMethod.paraType
            if (paraType.isString().not() && simpleMethod.returnType.isString()) {
                method.isAccessible = true
                val mapper = method.asMapper<Any?, String>("")
                val test = FieldSelector.of(method.getAnnotation(Fields::class.java)).toTest()
                attrs.asSequence()
                    .filter { test(it.field) && it.field.type == paraType }
                    .forEach { it.mapper = mapper }
            }
        }
    }
}