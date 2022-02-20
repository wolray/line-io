package com.github.wolray.line.io

import com.alibaba.fastjson.JSON
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*
import java.util.function.Function

/**
 * @author wolray
 */
abstract class LineReader<S, V, T> protected constructor(protected val function: Function<V, T>) {

    open fun read(source: S) = Session(source)

    abstract fun toIterator(source: S): Iterator<V>

    protected open fun reorder(slots: IntArray) {
        throw NotImplementedError()
    }

    interface SizedIterator<T> : Iterator<T> {
        fun size(): Long
    }

    open class Text<T> internal constructor(parser: Function<String, T>) :
        LineReader<InputStream, String, T>(parser) {
        override fun toIterator(source: InputStream): Iterator<String> {
            return BufferedReader(InputStreamReader(source)).lineSequence().iterator()
        }
    }

    class Excel<T> internal constructor(
        private val sheetIndex: Int,
        converter: ValuesConverter.Excel<T>
    ) : LineReader<InputStream, Row, T>(converter) {

        override fun toIterator(source: InputStream): Iterator<Row> {
            return try {
                XSSFWorkbook(source).getSheetAt(sheetIndex).iterator()
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

        fun columnsRange(startInclusive: Int, endExclusive: Int) = apply {
            slots = IntRange(startInclusive, endExclusive).toList().toIntArray()
        }

        fun columns(vararg slots: Int) = apply { this.slots = slots }

        fun columns(excelCols: String?) = apply {
            if (excelCols.isNullOrEmpty()) {
                slots = IntArray(0)
            } else {
                val split = excelCols.split(",".toRegex())
                val a = 'A'
                slots = IntArray(split.size).apply {
                    for (i in split.indices) {
                        val col = split[i].trim { it <= ' ' }
                        val j = col[0] - a
                        if (col.length > 1) {
                            this[i] = (j + 1) * 26 + col[1].code - a.code
                        } else {
                            this[i] = j
                        }
                    }
                }
            }
        }

        protected open fun preprocess(iterator: Iterator<V>) {
            slots.testIf { isNotEmpty() }
                .call { reorder(this) }
        }

        private fun getIterator() = toIterator(source).apply {
            repeat(skip) { next() }
            preprocess(this)
        }

        fun sequence(): Sequence<T> {
            return Sequence(::getIterator).map(function::apply)
        }

        fun stream(): DataStream<T> = DataStream.of {
            getIterator().run {
                toStream(if (this is SizedIterator) size() else null)
            }.map(function)
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

        @JvmStatic
        fun toInputStream(file: String): InputStream? {
            return try {
                FileInputStream(file)
            } catch (e: FileNotFoundException) {
                null
            }
        }
    }
}
