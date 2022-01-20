package com.github.wolray.line.io

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import java.lang.reflect.Constructor
import java.util.function.Function
import java.util.function.ToIntFunction
import kotlin.math.min

/**
 * @author wolray
 */
open class ValuesConverter<V, T>(
    protected val typeValues: TypeValues<T>,
    private val sizeGetter: ToIntFunction<V>
) {
    protected val attrs: Array<TypeValues.Attr> = typeValues.toAttrs()
    private val constructor: Constructor<T>
    private var filler: (T, V) -> Unit

    init {
        constructor = initConstructor(typeValues.type)
        initParsers()
        filler = fillAll()
    }

    private fun initConstructor(type: Class<T>): Constructor<T> {
        return try {
            type.getConstructor()
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        }
    }

    private fun initParsers() {
        val parserMap: MutableMap<Class<*>?, (String) -> Any?> = HashMap(11)
        parserMap[String::class.java] = { it }
        parserMap[Char::class.javaPrimitiveType] = { it[0] }
        parserMap[Char::class.javaObjectType] = { it[0] }
        parserMap[Boolean::class.javaPrimitiveType] = { it.toBoolean() }
        parserMap[Boolean::class.javaObjectType] = { it.toBoolean() }
        parserMap[Int::class.javaPrimitiveType] = { it.toInt() }
        parserMap[Int::class.javaObjectType] = { it.toInt() }
        parserMap[Long::class.javaPrimitiveType] = { it.toLong() }
        parserMap[Long::class.javaObjectType] = { it.toLong() }
        parserMap[Double::class.javaPrimitiveType] = { it.toDouble() }
        parserMap[Double::class.javaObjectType] = { it.toDouble() }
        for (attr in attrs) {
            val type = attr.field.type
            attr.parser = if (type.isEnum) {
                { parseEnum(type, it) }
            } else {
                parserMap[type]
            }
        }
        TypeValues.processSimpleMethods(typeValues.type) { processMethod(it) }
        checkParsers()
    }

    fun processMethod(simpleMethod: TypeValues.SimpleMethod) {
        val method = simpleMethod.method
        val returnType = simpleMethod.returnType
        if (simpleMethod.paraType == String::class.java) {
            val fields = method.getAnnotation(Fields::class.java)
            val selector = TypeValues.makeSelector(fields)
            val seq = attrs.asSequence().filter { selector.invoke(it.field) }
            method.isAccessible = true
            if (returnType == String::class.java) {
                val mapper = { s: String -> TypeValues.invoke(method, s) as String }
                seq.forEach { it.mapper = mapper }
            } else {
                val parser = { s: String -> TypeValues.invoke(method, s) }
                seq.filter { it.field.type == returnType }
                    .forEach { it.parser = parser }
            }
        }
    }

    private fun checkParsers() {
        for (a in attrs) {
            if (a.parser == null) {
                val type = a.field.type
                val name = type.name
                val simpleName = type.simpleName
                val clazz = typeValues.type.simpleName
                val msg = "cannot parse $name, please add a static method (String -> $simpleName) inside $clazz"
                throw IllegalStateException(msg)
            }
            a.composeMapper()
        }
    }

    fun resetOrder(slots: IntArray) {
        filler = fillBySlots(slots)
    }

    private fun fillAll(): (T, V) -> Unit {
        val attrs = attrs
        val len = attrs.size
        return { t, v ->
            val max = min(len, sizeGetter.applyAsInt(v))
            for (i in 0 until max) {
                fillAt(t, v, i, attrs[i])
            }
        }
    }

    private fun fillBySlots(slots: IntArray): (T, V) -> Unit {
        val attrs = attrs
        val len = min(attrs.size, slots.size)
        return { t, v ->
            val max = min(len, sizeGetter.applyAsInt(v))
            for (i in 0 until max) {
                fillAt(t, v, slots[i], attrs[i])
            }
        }
    }

    private fun fillAt(t: T, values: V, index: Int, attr: TypeValues.Attr) {
        attr[t] = convertAt(values, index, attr)
    }

    protected open fun convertAt(values: V, index: Int, attr: TypeValues.Attr): Any? {
        throw UnsupportedOperationException()
    }

    fun convert(v: V): T {
        return try {
            constructor.newInstance().also { filler.invoke(it, v) }
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }
    }

    class Text<T>(typeValues: TypeValues<T>) :
        ValuesConverter<Array<String>, T>(typeValues, { it.size }) {
        override fun convertAt(values: Array<String>, index: Int, attr: TypeValues.Attr): Any? {
            return attr.parse(values[index])
        }

        fun toParser(sep: String): Function<String, T> {
            val reg = sep.toRegex()
            return Function { s -> convert(s.split(reg).toTypedArray()) }
        }
    }

    class Excel<T>(typeValues: TypeValues<T>) :
        ValuesConverter<Row, T>(typeValues, { it.lastCellNum.toInt() }) {

        init {
            val functionMap: MutableMap<Class<*>?, (Cell) -> Any> = HashMap(9)
            functionMap[String::class.java] = { it.stringCellValue }
            functionMap[Boolean::class.javaPrimitiveType] = { it.booleanCellValue }
            functionMap[Boolean::class.javaObjectType] = { it.booleanCellValue }
            functionMap[Int::class.javaPrimitiveType] = { it.numericCellValue.toInt() }
            functionMap[Int::class.javaObjectType] = { it.numericCellValue.toInt() }
            functionMap[Long::class.javaPrimitiveType] = { it.numericCellValue.toLong() }
            functionMap[Long::class.javaObjectType] = { it.numericCellValue.toLong() }
            functionMap[Double::class.javaPrimitiveType] = { it.numericCellValue }
            functionMap[Double::class.javaObjectType] = { it.numericCellValue }
            val df = DataFormatter()
            for (attr in attrs) {
                val function = functionMap[attr.field.type]
                attr.function =
                    if (function != null) {
                        { function.invoke(it as Cell) }
                    } else {
                        { attr.parse(df.formatCellValue(it as Cell)) }
                    }
            }
        }

        override fun convertAt(values: Row, index: Int, attr: TypeValues.Attr): Any? {
            val cell = values.getCell(index)
            return attr.convert(cell)
        }
    }

    companion object {
        private fun <T : Enum<T>?> parseEnum(type: Class<*>, s: String): Any? {
            return try {
                java.lang.Enum.valueOf(type as Class<T>, s)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
