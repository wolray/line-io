package com.github.wolray.line.io

import com.alibaba.fastjson.JSON
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*
import java.util.*
import java.util.function.Function
import java.util.function.Supplier
import kotlin.streams.asStream

/**
 * @author wolray
 */
abstract class LineReader<S, V, T>(
    private val isByValues: Boolean,
    private val function: Function<V, T>
) {
    abstract fun toIterator(source: S): Iterator<V>

    fun read(source: S) = if (isByValues) ValuesSession(source) else Session(source)

    protected open fun splitHeader(v: V): List<String> = notImplError()
    protected open fun errorColMsg(col: String, v: V): String = notImplError()
    protected open fun reorder(slots: IntArray): Unit = notImplError()

    inner class ValuesSession(source: S) : Session(source) {
        private var slots: IntArray? = null
        private var cols: Array<String>? = null

        override fun columns(vararg index: Int) = apply { slots = index }
        override fun columnsBefore(index: Int) = columnsRange(0, index)
        override fun columnsRange(start: Int, before: Int) = apply { slots = rangeOf(start, before) }

        override fun columns(excelCols: String) = apply {
            slots = if (excelCols.isEmpty()) IntArray(0) else {
                val a = 'A'
                excelCols.split(",".toRegex())
                    .map {
                        it.trim().fold(0) { acc, c -> acc * 26 + (c - a + 1) } - 1
                    }
                    .toIntArray()
            }
        }

        override fun csvHeader(vararg useCols: String) = apply { cols = arrayOf(*useCols) }

        override fun preprocess(iterator: Iterator<V>) {
            slots = cols?.takeIf { it.isNotEmpty() }?.let {
                matchHeader(iterator.next(), it)
            } ?: slots
            slots?.takeIf { it.isNotEmpty() }?.also { reorder(it) }
        }

        private fun matchHeader(v: V, header: Array<String>): IntArray {
            val split = splitHeader(v)
            return header.map {
                split.indexOf(it).apply {
                    if (this < 0) throw NoSuchElementException(errorColMsg(it, v))
                }
            }.toIntArray()
        }
    }

    open inner class Session(private val source: S) : Chainable<Session> {
        private var errorType: Class<out Throwable>? = null
        private var skip: Int = 0
        override val self get() = this

        fun ignoreError(type: Class<out Throwable>) = apply { errorType = type }
        fun skipLines(n: Int) = apply { skip = n }

        open fun columns(vararg index: Int): Session = notImplError()
        open fun columnsBefore(index: Int): Session = notImplError()
        open fun columnsRange(start: Int, before: Int): Session = notImplError()
        open fun columns(excelCols: String): Session = notImplError()
        open fun csvHeader(vararg useCols: String): Session = notImplError()
        open fun preprocess(iterator: Iterator<V>) {}

        private fun getIterator(): Iterator<V> {
            return try {
                toIterator(source).apply {
                    if (skip > 0) {
                        repeat(skip) { next() }
                    }
                    preprocess(this)
                }
            } catch (e: Throwable) {
                if (errorType?.isAssignableFrom(e.javaClass) != true) throw e
                else Collections.emptyIterator()
            }
        }

        fun sequence(): Sequence<T> {
            return Sequence(::getIterator).map(function::apply)
        }

        fun <R> sequence(function: Function<Sequence<T>, R>): R {
            return function.apply(sequence())
        }

        fun stream(): DataStream<T> = DataStream.of {
            getIterator().asSequence().asStream().map(function)
        }
    }

    abstract class Is<V, T>(isByValues: Boolean, function: Function<V, T>) :
        LineReader<Supplier<InputStream>, V, T>(isByValues, function) {

        fun read(file: String) = read { FileInputStream(file) }
        fun read(file: File) = read { FileInputStream(file) }
        fun read(cls: Class<*>, resource: String) = read { cls.getResourceAsStream(resource)!! }
    }

    open class Text<T>(isByValues: Boolean, function: Function<String, T>) :
        Is<String, T>(isByValues, function) {

        override fun toIterator(source: Supplier<InputStream>): Iterator<String> {
            return BufferedReader(InputStreamReader(source.get())).lineSequence().iterator()
        }
    }

    class Excel<T> internal constructor(
        private val sheetIndex: Int,
        private val converter: ValuesConverter.Excel<T>
    ) : Is<Row, T>(true, converter) {

        override fun toIterator(source: Supplier<InputStream>): Iterator<Row> {
            return try {
                XSSFWorkbook(source.get()).getSheetAt(sheetIndex).iterator()
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }

        override fun splitHeader(v: Row): List<String> = v.map { it.stringCellValue }
        override fun errorColMsg(col: String, v: Row): String = col
        override fun reorder(slots: IntArray) = converter.resetOrder(slots)
    }

    companion object {
        @JvmStatic
        fun <T> simple(parser: Function<String, T>) = Text(false, parser)

        @JvmStatic
        fun <T> byJson(type: Class<T>) = Text(false) { JSON.parseObject(it, type) }

        @JvmStatic
        fun <T> byCsv(sep: String, type: Class<T>): CsvReader<T> {
            return CsvReader(ValuesConverter.Text(TypeValues(type)), sep)
        }

        @JvmStatic
        fun <T> byExcel(type: Class<T>) = byExcel(0, type)

        @JvmStatic
        fun <T> byExcel(sheetIndex: Int, type: Class<T>): Excel<T> {
            return Excel(sheetIndex, ValuesConverter.Excel(TypeValues(type)))
        }

        private fun <T> notImplError(): T = throw NotImplementedError()

        private fun rangeOf(start: Int, before: Int): IntArray {
            return (start until before).toList().toIntArray()
        }
    }
}
