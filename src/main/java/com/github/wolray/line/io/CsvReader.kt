package com.github.wolray.line.io

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.function.Supplier

/**
 * @author wolray
 */
class CsvReader<T> internal constructor(
    private val converter: ValuesConverter.Text<T>,
    private val sep: String
) : LineReader.Is<Array<String>, T>(true, converter) {

    override fun toIterator(source: Supplier<InputStream>): Iterator<Array<String>> {
        return BufferedReader(InputStreamReader(source.get())).lineSequence()
            .map { (it as java.lang.String).split(sep) }
            .iterator()
    }

    override fun splitHeader(v: Array<String>): List<String> = v.toList()
    override fun errorColMsg(col: String, v: Array<String>): String = "$col not in ${v.contentToString()}"
    override fun reorder(slots: IntArray) = converter.resetOrder(slots)

    companion object {
        @JvmStatic
        fun <T> of(sep: String, type: Class<T>) = byCsv(sep, type)
    }
}
