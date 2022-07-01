package com.github.wolray.line.io

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
    override fun errorColMsg(col: String, header: List<String>) = "$col not in $header"

    companion object {
        @JvmStatic
        fun <T> of(sep: String, type: Class<T>) = byCsv(sep, type)
    }
}
