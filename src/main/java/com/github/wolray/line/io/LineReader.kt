package com.github.wolray.line.io

import com.alibaba.fastjson.JSON
import com.github.wolray.line.io.TextScope.toSequence
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.util.*
import java.util.function.Function
import java.util.function.Supplier
import kotlin.streams.asStream

/**
 * @author wolray
 */
abstract class LineReader<S, V, T>(val function: Function<V, T>) : IReader<S, V, T> {

    override fun read(source: S) = Session(source)
    override fun invoke(v: V): T = function.apply(v)

    open inner class Session(private val source: S) : Chainable<Session> {
        override val self get() = this
        private var errorType: Class<out Throwable>? = null
        private var skip: Int = 0
        protected var slots: IntArray? = null
        protected var cols: Array<String>? = null

        fun ignoreError(type: Class<out Throwable>) = apply { errorType = type }
        fun skipLines(n: Int) = apply { skip = n }

        fun columns(vararg col: String) = apply { cols = arrayOf(*col) }
        fun columns(vararg index: Int) = apply { slots = index }
        fun columnsBefore(index: Int) = columnsRange(0, index)

        fun columnsRange(start: Int, before: Int) = apply {
            slots = (start until before).toList().toIntArray()
        }

        fun excelColumns(excelCols: String) = apply {
            slots = if (excelCols.isEmpty()) intArrayOf() else {
                excelCols.split(",").map(::excelIndex).toIntArray()
            }
        }

        private fun excelIndex(col: String): Int {
            return col.trim().fold(0) { acc, c -> acc * 26 + (c - A + 1) } - 1
        }

        protected open fun preprocess(iterator: Iterator<V>) {}

        private fun getIterator(): Iterator<V> {
            return try {
                toIterator(source).apply {
                    if (skip > 0) repeat(skip) { next() }
                    preprocess(this)
                }
            } catch (e: Throwable) {
                if (errorType?.isAssignableFrom(e.javaClass) != true) throw e
                else Collections.emptyIterator()
            }
        }

        fun sequence(): Sequence<T> {
            return Sequence(::getIterator).map(this@LineReader)
        }

        fun <R> sequence(function: Function<Sequence<T>, R>): R {
            return function.apply(sequence())
        }

        fun stream(): DataStream<T> = DataStream.of { sequence().asStream() }
    }

    open class Simple<T>(function: Function<String, T>) :
        LineReader<Text, String, T>(function), IReader.Is<String, T> {

        override fun toIterator(source: Text): Iterator<String> {
            return source.toSequence().iterator()
        }
    }

    class Excel<T> internal constructor(
        private val sheetIndex: Int,
        converter: ValuesConverter.Excel<T>
    ) : ValuesReader<Text, Row, T>(converter), IReader.Is<Row, T> {

        override fun toIterator(source: Supplier<InputStream>): Iterator<Row> =
            XSSFWorkbook(source.get()).getSheetAt(sheetIndex).iterator()

        override fun splitHeader(iterator: Iterator<Row>) = iterator.next().map { it.stringCellValue }
    }

    companion object {
        private const val A = 'A'

        @JvmStatic
        fun <T> simple(parser: Function<String, T>) = Simple(parser)

        @JvmStatic
        fun <T> byJson(type: Class<T>) = Simple { JSON.parseObject(it, type) }

        @JvmStatic
        fun <T> byCsv(sep: String, type: Class<T>): CsvReader<T> {
            return CsvReader(ValuesConverter.Csv(TypeValues(type)), sep)
        }

        @JvmStatic
        fun <T> byExcel(type: Class<T>) = byExcel(0, type)

        @JvmStatic
        fun <T> byExcel(sheetIndex: Int, type: Class<T>): Excel<T> {
            return Excel(sheetIndex, ValuesConverter.Excel(TypeValues(type)))
        }
    }
}
