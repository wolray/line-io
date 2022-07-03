package com.github.wolray.line.io

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import kotlin.math.min

/**
 * @author wolray
 */
abstract class ValuesConverter<V, T>(val typeValues: TypeValues<T>) : (V) -> T {
    private val constructor: Constructor<T> = typeValues.type.getConstructor()
    private var filler: (T, V) -> Unit

    init {
        filler = fillAll()
        TypeValues.processSimpleMethods(typeValues.type, ::processMethod)
    }

    abstract fun sizeOf(values: V): Int

    abstract fun convertAt(values: V, slot: Int, index: Int): Any?

    open fun processMethod(method: TypeValues.SimpleMethod) {}

    override fun invoke(v: V): T {
        val t = constructor.newInstance()
        filler(t, v)
        return t
    }

    fun resetOrder(slots: IntArray) {
        filler = fillBySlots(slots)
    }

    private fun fillAll(): (T, V) -> Unit {
        val len = typeValues.size
        return { t, v ->
            val max = min(len, sizeOf(v))
            for (i in 0 until max) {
                fillAt(t, v, i, i)
            }
        }
    }

    private fun fillBySlots(slots: IntArray): (T, V) -> Unit {
        val len = min(typeValues.size, slots.size)
        return { t, v ->
            val max = min(len, sizeOf(v))
            for (i in 0 until max) {
                fillAt(t, v, slots[i], i)
            }
        }
    }

    private fun fillAt(t: T, values: V, slot: Int, index: Int) {
        val f = typeValues.values[index]
        try {
            val any = convertAt(values, slot, index)
            f[t] = any
        } catch (e: Throwable) {
            val str = if (values is Array<*>) {
                values.joinToString(DataMapper.DEFAULT_SEP)
            } else {
                values.toString()
            }
            val message = "[$str] at col $slot for field ${f.name}: ${f.type}"
            throw IllegalArgumentException(message, e)
        }
    }

    class Attr<E>(val field: Field, var mapper: (E) -> Any?)

    abstract class Split<V, E, T>(typeValues: TypeValues<T>) : ValuesConverter<V, T>(typeValues) {
        val attrs: Array<Attr<E>> = toAttrs()

        abstract fun toStr(e: E): String
        abstract fun toBool(e: E): Boolean
        abstract fun toInt(e: E): Int
        abstract fun toDouble(e: E): Double
        abstract fun toLong(e: E): Long

        abstract fun getAt(values: V, slot: Int): E

        override fun convertAt(values: V, slot: Int, index: Int): Any? {
            return try {
                attrs[index].mapper(getAt(values, slot))
            } catch (e: Throwable) {
                null
            }
        }

        private fun toAttrs(): Array<Attr<E>> {
            return typeValues.values.asSequence()
                .map {
                    val mapper = when (val type = it.type) {
                        String::class.java -> ::toStr
                        Boolean::class.javaObjectType, Boolean::class.javaPrimitiveType -> ::toBool
                        Int::class.javaObjectType, Int::class.javaPrimitiveType -> ::toInt
                        Double::class.javaObjectType, Double::class.javaPrimitiveType -> ::toDouble
                        Long::class.javaObjectType, Long::class.javaPrimitiveType -> ::toLong
                        else -> {
                            throw IllegalStateException(
                                "cannot parse $type, " +
                                    "please add a static method (String -> ${type.simpleName}) inside ${typeValues.type}"
                            )
                        }
                    }
                    Attr(it, mapper)
                }
                .toList()
                .toTypedArray()
        }
    }

    class Csv<T>(typeValues: TypeValues<T>) : Split<List<String>, String, T>(typeValues) {

        override fun sizeOf(values: List<String>): Int = values.size
        override fun getAt(values: List<String>, slot: Int): String = values[slot]

        override fun toStr(e: String): String = e
        override fun toBool(e: String): Boolean = e.toBoolean()
        override fun toInt(e: String): Int = e.toInt()
        override fun toDouble(e: String): Double = e.toDouble()
        override fun toLong(e: String): Long = e.toLong()

        override fun processMethod(method: TypeValues.SimpleMethod) {
            val m = method.method
            val returnType = m.returnType
            if (method.paraType == String::class.java) {
                val test = FieldSelector.of(m.getAnnotation(Fields::class.java)).toTest()
                val seq = attrs.asSequence().filter { test(it.field) }
                m.isAccessible = true
                if (returnType == String::class.java) {
                    val mapper: (String) -> String? = { TypeValues.call(m, it) }
                    seq.forEach {
                        val old = it.mapper
                        it.mapper = { s -> old(mapper(s)!!) }
                    }
                } else {
                    val mapper: (String) -> Any? = { TypeValues.call(m, it) }
                    seq
                        .filter { it.field.type == returnType }
                        .forEach { it.mapper = mapper }
                }
            }
        }

        fun toParser(sep: String): (String) -> T {
            return { this(it.split(sep)) }
        }
    }

    class Excel<T>(typeValues: TypeValues<T>) : Split<Row, Cell, T>(typeValues) {

        override fun sizeOf(values: Row): Int = values.lastCellNum.toInt()
        override fun getAt(values: Row, slot: Int): Cell = values.getCell(slot)

        override fun toStr(e: Cell): String = e.stringCellValue
        override fun toBool(e: Cell): Boolean = e.booleanCellValue
        override fun toInt(e: Cell): Int = e.numericCellValue.toInt()
        override fun toDouble(e: Cell): Double = e.numericCellValue
        override fun toLong(e: Cell): Long = e.numericCellValue.toLong()
    }
}