package com.github.wolray.line.io

/**
 * @author wolray
 */
abstract class Cacheable<T, S> {
    abstract fun from(session: LineReader<*, *, T>.Session): S
    abstract fun toList(): List<T>
    abstract fun after(): S

    fun cacheBy(cache: Cache<T>): S {
        return if (cache.exists()) {
            from(cache.read())
        } else {
            val ts = toList()
            if (ts.isNotEmpty()) {
                cache.write(ts)
            }
            after()
        }
    }

    private fun cacheFile(
        file: String, suffix: String,
        reader: () -> LineReader.Text<T>,
        writer: () -> LineWriter<T>
    ): S {
        val f = if (file.endsWith(suffix)) file else file + suffix
        val input = LineReader.toInputStream(f)
        return cacheBy(object : Cache<T> {
            override fun exists() = input != null
            override fun read() = reader.invoke().read(input!!)
            override fun write(ts: List<T>) = writer.invoke().write(f).with(ts)
        })
    }

    @JvmOverloads
    fun cacheCsv(file: String, type: Class<T>, sep: String = ","): S {
        return cacheCsv(file, DataMapper.simple(type, sep))
    }

    fun cacheCsv(file: String, mapper: DataMapper<T>): S {
        return cacheFile(file, ".csv", mapper::toReader, mapper::toWriter)
    }

    fun cacheJson(file: String, type: Class<T>): S {
        return cacheFile(file, ".txt", { LineReader.byJson(type) }, { LineWriter.byJson() })
    }

    interface Cache<T> {
        fun exists(): Boolean
        fun read(): LineReader<*, *, T>.Session
        fun write(ts: List<T>)
    }
}
