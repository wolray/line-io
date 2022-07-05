package com.github.wolray.line.io

import com.github.wolray.line.io.TextScope.toSequence

/**
 * @author wolray
 */
class CsvReader<T> internal constructor(
    converter: ValuesConverter.Csv<T>,
    private val sep: String
) : ValuesReader<Text, List<String>, T>(converter), IReader.Is<List<String>, T> {

    override fun toIterator(source: Text): Iterator<List<String>> {
        return source.toSequence().map(::split).iterator()
    }

    private fun split(s: String) = s.split(sep, limit = limit)

    override fun splitHeader(iterator: Iterator<List<String>>) = iterator.next()

    companion object {
        @JvmStatic
        fun <T> of(sep: String, type: Class<T>) = byCsv(sep, type)
    }
}
