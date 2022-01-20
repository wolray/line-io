package com.github.wolray.line.io

import java.util.*
import java.util.function.Function

/**
 * @author wolray
 */
class ValuesJoiner<T>(private val typeValues: TypeValues<T>) {
    private val attrs: Array<TypeValues.Attr> = typeValues.toAttrs()

    init {
        initFormatters()
    }

    private fun initFormatters() {
        val toString = { o: Any? -> o.toString() }
        for (attr in attrs) {
            attr.formatter = toString
        }
        TypeValues.processSimpleMethods(typeValues.type) { processMethod(it) }
    }

    fun processMethod(simpleMethod: TypeValues.SimpleMethod) {
        val method = simpleMethod.method
        val paraType = simpleMethod.paraType
        if (paraType != String::class.java && simpleMethod.returnType == String::class.java) {
            val fields = method.getAnnotation(Fields::class.java)
            val selector = TypeValues.makeSelector(fields)
            method.isAccessible = true
            val function = { s: Any? -> TypeValues.invoke(method, s) as String }
            attrs.asSequence()
                .filter { it.field.type == paraType && selector.invoke(it.field) }
                .forEach { a -> a.formatter = function }
        }
    }

    fun join(sep: String, function: Function<TypeValues.Attr, String>): String {
        val joiner = StringJoiner(sep)
        for (attr in attrs) {
            joiner.add(function.apply(attr))
        }
        return joiner.toString()
    }

    fun toFormatter(sep: String): Function<T, String> {
        return Function { t -> join(sep) { it.format(it[t]) } }
    }
}
