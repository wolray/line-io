package com.github.wolray.line.io

import com.github.wolray.line.io.MethodScope.annotation
import com.github.wolray.line.io.MethodScope.asMapper
import com.github.wolray.line.io.TypeScope.isBool
import com.github.wolray.line.io.TypeScope.isNumber
import com.github.wolray.line.io.TypeScope.isString
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import kotlin.math.min

/**
 * @author wolray
 */
abstract class ValuesConverter<V, E, T>(val typeValues: TypeValues<T>) : (V) -> T {
    val attrs: Array<Attr<E>> = toAttrs()
    private val constructor: Constructor<T> = typeValues.type.getConstructor()
    private var filler: (T, V) -> Unit

    init {
        filler = fillAll()
        TypeValues.processSimpleMethods(typeValues.type, ::processMethod)
    }

    abstract fun sizeOf(values: V): Int
    abstract fun getAt(values: V, slot: Int): E
    abstract fun str(values: V): String

    abstract fun toStr(e: E): String
    abstract fun toBool(e: E): Boolean
    abstract fun toInt(e: E): Int
    abstract fun toDouble(e: E): Double
    abstract fun toLong(e: E): Long

    private fun toAttrs(): Array<Attr<E>> = typeValues.values.asSequence()
        .map {
            val t = it.type
            val mapper = when {
                t.isString() -> ::toStr
                t.isBool() -> ::toBool
                t.isNumber<Int>() -> ::toInt
                t.isNumber<Double>() -> ::toDouble
                t.isNumber<Long>() -> ::toLong
                else -> {
                    throw IllegalStateException(
                        "cannot parse $t, please add a static method " +
                            "(String -> ${t.simpleName}) inside ${typeValues.type}"
                    )
                }
            }
            Attr(it, mapper)
        }
        .toList()
        .toTypedArray()

    private fun convertAt(values: V, slot: Int, attr: Attr<E>): Any? {
        return try {
            attr.mapper(getAt(values, slot))
        } catch (e: Throwable) {
            null
        }
    }

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
                fillAt(t, v, i, attrs[i])
            }
        }
    }

    private fun fillBySlots(slots: IntArray): (T, V) -> Unit {
        val len = min(typeValues.size, slots.size)
        return { t, v ->
            val max = min(len, sizeOf(v))
            for (i in 0 until max) {
                fillAt(t, v, slots[i], attrs[i])
            }
        }
    }

    private fun fillAt(t: T, values: V, slot: Int, attr: Attr<E>) {
        val f = attr.field
        try {
            f[t] = convertAt(values, slot, attr)
        } catch (e: Throwable) {
            val s = str(values)
            val message = "[$s] at col $slot for field ${f.name}: ${f.type}"
            throw IllegalArgumentException(message, e)
        }
    }

    class Attr<E>(val field: Field, var mapper: (E) -> Any?)

    class Csv<T>(typeValues: TypeValues<T>) : ValuesConverter<List<String>, String, T>(typeValues) {

        override fun sizeOf(values: List<String>): Int = values.size
        override fun getAt(values: List<String>, slot: Int): String = values[slot]
        override fun str(values: List<String>): String = values.toString()

        override fun toStr(e: String): String = e
        override fun toBool(e: String): Boolean = e.toBoolean()
        override fun toInt(e: String): Int = e.toInt()
        override fun toDouble(e: String): Double = e.toDouble()
        override fun toLong(e: String): Long = e.toLong()

        override fun processMethod(method: TypeValues.SimpleMethod) {
            val m = method.method
            val returnType = m.returnType
            if (method.paraType.isString()) {
                val test = FieldSelector.of(m.annotation()).toTest()
                val seq = attrs.asSequence().filter { test(it.field) }
                if (returnType.isString()) {
                    val mapper = m.asMapper<String, String>("")
                    seq.forEach {
                        val old = it.mapper
                        it.mapper = { s -> old(mapper(s)) }
                    }
                } else {
                    val mapper = m.asMapper<String, Any>()
                    seq
                        .filter { it.field.type == returnType }
                        .forEach { it.mapper = mapper }
                }
            }
        }

        fun toParser(sep: String): (String) -> T = { this(it.split(sep)) }
    }

    class Excel<T>(typeValues: TypeValues<T>) : ValuesConverter<Row, Cell, T>(typeValues) {

        override fun sizeOf(values: Row): Int = values.lastCellNum.toInt()
        override fun getAt(values: Row, slot: Int): Cell = values.getCell(slot)
        override fun str(values: Row): String = values.joinToString { it.toString() }

        override fun toStr(e: Cell): String = e.stringCellValue
        override fun toBool(e: Cell): Boolean = e.booleanCellValue
        override fun toInt(e: Cell): Int = e.numericCellValue.toInt()
        override fun toDouble(e: Cell): Double = e.numericCellValue
        override fun toLong(e: Cell): Long = e.numericCellValue.toLong()
    }
}