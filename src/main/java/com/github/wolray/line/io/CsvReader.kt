package com.github.wolray.line.io

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.function.Supplier

/**
 * @author wolray
 */
class CsvReader<T> internal constructor(
    private val converter: ValuesConverter.Text<T>,
    private val sep: String
) : LineReader.Text<T>(converter.toParser(sep)) {

    fun read(file: String) = Session { FileInputStream(file) }

    fun read(file: File) = Session { FileInputStream(file) }

    override fun read(source: Supplier<InputStream>) = Session(source)

    override fun reorder(slots: IntArray) = converter.resetOrder(slots)

    private fun setHeader(s: String, header: Array<String>) {
        val list = s.split(sep.toRegex())
        header
            .map {
                list.indexOf(it).apply {
                    if (this < 0) throw NoSuchElementException(it)
                }
            }
            .toIntArray()
            .also { reorder(it) }
    }

    inner class Session internal constructor(input: Supplier<InputStream>) :
        LineReader<Supplier<InputStream>, String, T>.Session(input),
        Chainable<Session> {
        override val self: Session = this
        private var cols: Array<String>? = null

        override fun skipLines(n: Int) = apply { super.skipLines(n) }

        fun csvHeader(vararg useCols: String) = apply { cols = arrayOf(*useCols) }

        override fun preprocess(iterator: Iterator<String>) {
            cols?.also { if (it.isNotEmpty()) setHeader(iterator.next(), it) }
                ?: super.preprocess(iterator)
        }
    }

    companion object {
        @JvmStatic
        fun <T> of(sep: String, type: Class<T>) = byCsv(sep, type)
    }
}
