package com.github.wolray.line.io

import java.io.BufferedWriter
import java.io.IOException

/**
 * @author wolray
 */
class CsvWriter<T> internal constructor(
    private val joiner: ValuesJoiner<T>,
    private val sep: String
) : LineWriter<T>(joiner.toFormatter(sep)) {

    override fun write(iterable: Iterable<T>) = Session(iterable)

    inner class Session internal constructor(iterable: Iterable<T>) :
        LineWriter<T>.Session(iterable) {
        private var utf8 = false

        fun markUtf8(): Session {
            utf8 = true
            return this
        }

        fun withHeader(): Session {
            addHeader(joiner.join(sep) { it.field.name })
            return this
        }

        fun columnNames(vararg names: String): Session {
            if (names.isNotEmpty()) {
                addHeader(names.joinToString(sep))
            }
            return this
        }

        @Throws(IOException::class)
        override fun preprocess(file: String, bw: BufferedWriter) {
            if (utf8 && file.endsWith(".csv")) {
                bw.write('\ufeff'.code)
            }
        }
    }
}