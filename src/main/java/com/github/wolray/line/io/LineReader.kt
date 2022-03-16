package com.github.wolray.line.io

import com.alibaba.fastjson.JSON
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*
import java.util.function.Function
import java.util.function.Supplier
import kotlin.streams.asStream

/**
 * @author wolray
 */
abstract class LineReader<S, V, T> protected constructor(protected val function: Function<V, T>) {

    open fun read(source: S) = Session(source)

    abstract fun toIterator(source: S): Iterator<V>

    protected open fun reorder(slots: IntArray) {
        throw NotImplementedError()
    }

    open class Text<T> internal constructor(parser: Function<String, T>) :
        LineReader<Supplier<InputStream>, String, T>(parser) {

        override fun toIterator(source: Supplier<InputStream>): Iterator<String> {
            return BufferedReader(InputStreamReader(source.get())).lineSequence().iterator()
        }
    }

    class Excel<T> internal constructor(
        private val sheetIndex: Int,
        converter: ValuesConverter.Excel<T>
    ) : LineReader<Supplier<InputStream>, Row, T>(converter) {

        override fun toIterator(source: Supplier<InputStream>): Iterator<Row> {
            return try {
                XSSFWorkbook(source.get()).getSheetAt(sheetIndex).iterator()
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }

        override fun reorder(slots: IntArray) {
            (function as ValuesConverter.Excel<T>).resetOrder(slots)
        }
    }

    open inner class Session(private val source: S) {
        private var skip: Int = 0
        private var slots: IntArray? = null

        open fun skipLines(n: Int) = apply { skip = n }

        fun columnsBefore(index: Int) = columnsRange(0, index)

        fun columnsRange(start: Int, endExclusive: Int) = apply {
            slots = IntRange(start, endExclusive).toList().toIntArray()
        }

        fun columns(vararg slots: Int) = apply { this.slots = slots }

        fun columns(excelCols: String?) = apply {
            slots = if (excelCols.isNullOrEmpty()) {
                IntArray(0)
            } else {
                val a = 'A'
                excelCols.split(",".toRegex())
                    .map {
                        val col = it.trim()
                        val j = it[0] - a
                        if (col.length > 1) {
                            (j + 1) * 26 + col[1].code - a.code
                        } else {
                            j
                        }
                    }.toIntArray()
            }
        }

        protected open fun preprocess(iterator: Iterator<V>) {
            slots?.also { if (it.isNotEmpty()) reorder(it) }
        }

        private fun getIterator() = toIterator(source).apply {
            repeat(skip) { next() }
            preprocess(this)
        }

        fun sequence(): Sequence<T> {
            return Sequence(::getIterator).map(function::apply)
        }

        fun stream(): DataStream<T> = DataStream.of {
            getIterator().asSequence().asStream().map(function)
        }
    }

    companion object {
        @JvmStatic
        fun <T> simple(parser: Function<String, T>) = Text(parser)

        @JvmStatic
        fun <T> byJson(type: Class<T>) = Text { JSON.parseObject(it, type) }

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
    }
}
