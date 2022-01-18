package com.github.wolray.line.io

import com.alibaba.fastjson.JSON
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*
import java.util.function.Function
import java.util.stream.Stream

/**
 * @author wolray
 */
open class LineReader<S, V, T> protected constructor(protected val function: Function<V, T>) {
    open fun read(source: S): Session {
        return Session(source, 0)
    }

    open fun read(source: S, skipLines: Int): Session {
        return Session(source, skipLines)
    }

    protected open fun toStream(source: S): Stream<V> {
        throw UnsupportedOperationException()
    }

    protected open fun reorder(slots: IntArray) {
        throw UnsupportedOperationException()
    }

    open class Text<T> internal constructor(parser: Function<String, T>) : LineReader<InputStream, String, T>(parser) {
        override fun toStream(source: InputStream): Stream<String> {
            return BufferedReader(InputStreamReader(source)).lines()
        }
    }

    class Excel<T> internal constructor(private val sheetIndex: Int, converter: ValuesConverter.Excel<T>) : LineReader<InputStream, Row, T>(converter) {
        override fun toStream(source: InputStream): Stream<Row> {
            return try {
                val workbook: Workbook = XSSFWorkbook(source)
                val sheet = workbook.getSheetAt(sheetIndex)
                StreamHelper.toStream(sheet.iterator(), null)
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }

        override fun reorder(slots: IntArray) {
            (function as ValuesConverter.Excel<T>).resetOrder(slots)
        }
    }

    open inner class Session(
            private val source: S,
            private val skipLines: Int) {
        private var slots: IntArray? = null

        fun columns(vararg slots: Int): Session {
            this.slots = slots
            return this
        }

        fun columns(excelCols: String): Session {
            slots = if (excelCols.isEmpty()) {
                IntArray(0)
            } else {
                val a = 'A'
                excelCols.split(",".toRegex())
                        .map {
                            val col = it.trim()
                            val j = col[0] - a
                            if (col.length > 1) (j + 1) * 26 + col[1].code - a.code else j
                        }
                        .toIntArray()
            }
            return this
        }

        protected open fun map(stream: Stream<V>): Stream<T> {
            slots?.takeIf(IntArray::isNotEmpty)?.also(::reorder)
            return stream.map(function)
        }

        fun stream(): DataStream<T> {
            return DataStream.of { map(toStream(source).skip(skipLines.toLong())) }
        }
    }

    companion object {
        @JvmStatic
        fun <T> simple(parser: Function<String, T>): Text<T> {
            return Text(parser)
        }

        @JvmStatic
        fun <T> byJson(type: Class<T>): Text<T> {
            return Text { JSON.parseObject(it, type) }
        }

        @JvmStatic
        fun <T> byCsv(sep: String, type: Class<T>): CsvReader<T> {
            return CsvReader(ValuesConverter.Text(TypeValues(type)), sep)
        }

        @JvmStatic
        fun <T> byExcel(type: Class<T>): Excel<T> {
            return byExcel(0, type)
        }

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
