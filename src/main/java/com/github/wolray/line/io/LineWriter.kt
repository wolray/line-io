package com.github.wolray.line.io

import com.alibaba.fastjson.JSON
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.IOException
import java.io.UncheckedIOException
import java.util.concurrent.CompletableFuture
import java.util.function.Function

/**
 * @author wolray
 */
class LineWriter<T>(private val formatter: Function<T, String>) {
    private var header: String? = null
    private var utf8Marker = false
    private var append = false

    fun header(header: String?): LineWriter<T> {
        this.header = header
        return this
    }

    fun markCsvAsUtf8(): LineWriter<T> {
        utf8Marker = true
        return this
    }

    fun appendToFile(): LineWriter<T> {
        append = true
        return this
    }

    fun writeAsync(iterable: Iterable<T>, file: String) {
        CompletableFuture.runAsync { write(iterable, file) }
    }

    fun write(iterable: Iterable<T>, file: String) {
        try {
            BufferedWriter(FileWriter(file, append)).use { bw ->
                if (!append) {
                    if (utf8Marker && file.endsWith(".csv")) {
                        bw.write('\ufeff'.code)
                    }
                    header?.also {
                        bw.write(it)
                        bw.write('\n'.code)
                    }
                }
                val formatter = formatter
                iterable.forEach {
                    bw.write(formatter.apply(it))
                    bw.write('\n'.code)
                }
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    companion object {
        @JvmStatic
        fun <T> byJson(): LineWriter<T> {
            return LineWriter { JSON.toJSONString(it) }
        }

        @JvmStatic
        fun <T> byCsv(sep: String, type: Class<T>, withHeader: Boolean): LineWriter<T> {
            val joiner = ValuesJoiner(TypeValues(type))
            val res = LineWriter(joiner.toFormatter(sep))
            if (withHeader) {
                res.header(joiner.join(sep) { it.field.name })
            }
            return res
        }

        @JvmStatic
        fun <T> byCsv(sep: String, type: Class<T>, vararg columns: String): LineWriter<T> {
            val joiner = ValuesJoiner(TypeValues(type))
            val res = LineWriter(joiner.toFormatter(sep))
            if (columns.isNotEmpty()) {
                res.header(java.lang.String.join(sep, *columns))
            }
            return res
        }
    }
}
