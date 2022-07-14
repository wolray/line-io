package com.github.wolray.line.io

import com.github.wolray.line.io.EmptyScope.ifNotEmpty
import java.lang.reflect.Field
import java.util.function.Function

/**
 * @author wolray
 */
class DataMapper<T> @JvmOverloads constructor(
    val typeValues: TypeValues<T>,
    val sep: String = DEFAULT_SEP
) {
    private val converter by lazy { ValuesConverter.Csv(typeValues) }
    private val joiner by lazy { ValuesJoiner(typeValues) }
    private val parser by lazy { toParser(sep) }
    private val formatter by lazy { toFormatter(sep) }

    fun newSep(sep: String): DataMapper<T> {
        return if (sep == this.sep) this else DataMapper(typeValues, sep)
    }

    fun toParser(sep: String): Function<String, T> = converter.toParser(sep)
    fun toFormatter(sep: String): Function<T, String> = joiner.toFormatter(sep)

    @JvmOverloads
    fun joinFields(sep: String = this.sep): String = joiner.joinFields(sep)

    @JvmOverloads
    fun toReader(sep: String = this.sep) = CsvReader(converter, sep)

    @JvmOverloads
    fun toWriter(sep: String = this.sep) = CsvWriter(joiner, sep)

    fun parse(s: String) = parser.apply(s)
    fun format(t: T) = formatter.apply(t)

    class Builder<T> internal constructor(private val type: Class<T>) {
        var pojo: Boolean = false
        var use: Array<String>? = null
        var omit: Array<String>? = null
        var useRegex: String? = null
        var omitRegex: String? = null

        private fun toFields() = Fields(
            pojo = pojo, use = use ?: emptyArray(), omit = omit ?: emptyArray(),
            useRegex = useRegex.orEmpty(), omitRegex = omitRegex.orEmpty()
        )

        fun pojo() = apply { pojo = true }
        fun use(vararg fields: String) = apply { use = arrayOf(*fields) }
        fun omit(vararg fields: String) = apply { omit = arrayOf(*fields) }
        fun useRegex(regex: String) = apply { useRegex = regex }
        fun omitRegex(regex: String) = apply { omitRegex = regex }
        fun build() = DataMapper(TypeValues(type, toFields()))
        fun build(sep: String) = DataMapper(TypeValues(type, toFields()), sep)
        fun toReader(sep: String) = build(sep).toReader()
        fun toWriter(sep: String) = build(sep).toWriter()
    }

    companion object {
        const val DEFAULT_SEP = "\u02cc"

        @JvmStatic
        @JvmOverloads
        fun <T> of(type: Class<T>, sep: String = DEFAULT_SEP) = DataMapper(TypeValues(type), sep)

        @JvmStatic
        fun <T> builder(type: Class<T>) = Builder(type)

        internal fun Fields?.toTest(): (Field) -> Boolean {
            this ?: return { true }
            use.ifNotEmpty {
                val set = toSet()
                return { set.contains(it.name) }
            }
            omit.ifNotEmpty {
                val set = toSet()
                return { !set.contains(it.name) }
            }
            useRegex.ifNotEmpty {
                val regex = toRegex()
                return { it.name.matches(regex) }
            }
            omitRegex.ifNotEmpty {
                val regex = toRegex()
                return { !it.name.matches(regex) }
            }
            return { true }
        }
    }
}
