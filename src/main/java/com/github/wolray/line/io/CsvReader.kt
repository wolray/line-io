package com.github.wolray.line.io

/**
 * @author wolray
 */
class CsvReader<T> internal constructor(
    converter: ValuesConverter.Csv<T>,
    private val sep: String
) : ValuesReader<Text, List<String>, T>(converter), IReader.Csv<T> {

    override fun split(s: String) = s.split(sep, limit = limit)

    override fun splitHeader(iterator: Iterator<List<String>>) = iterator.next()

    companion object {
        @JvmStatic
        fun <T> of(sep: String, type: Class<T>) = byCsv(sep, type)
    }
}
