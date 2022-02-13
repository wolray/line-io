package com.github.wolray.line.io

import java.util.function.Function

/**
 * @author wolray
 */
class DataMapper<T> @JvmOverloads constructor(
    val typeValues: TypeValues<T>,
    val sep: String = "\u02cc"
) {
    private val converter by lazy { ValuesConverter.Text(typeValues) }
    private val joiner by lazy { ValuesJoiner(typeValues) }
    private val parser by lazy { toParser(sep) }
    private val formatter by lazy { toFormatter(sep) }

    fun newSep(sep: String): DataMapper<T> {
        return if (sep == this.sep) this else DataMapper(typeValues, sep)
    }

    fun toParser(sep: String): Function<String, T> {
        return converter.toParser(sep)
    }

    fun toFormatter(sep: String): Function<T, String> {
        return joiner.toFormatter(sep)
    }

    @JvmOverloads
    fun toReader(sep: String = this.sep) = CsvReader(converter, sep)

    @JvmOverloads
    fun toWriter(sep: String = this.sep) = CsvWriter(joiner, sep)

    fun parse(s: String) = parser.apply(s)

    fun format(t: T) = formatter.apply(t)

    class Builder<T> internal constructor(private val type: Class<T>) {
        private val selector = FieldSelector()

        fun pojo(): Builder<T> {
            selector.pojo = true
            return this
        }

        fun use(vararg fields: String): Builder<T> {
            selector.use = fields
            return this
        }

        fun omit(vararg fields: String): Builder<T> {
            selector.omit = fields
            return this
        }

        fun useRegex(regex: String): Builder<T> {
            selector.useRegex = regex
            return this
        }

        fun omitRegex(regex: String): Builder<T> {
            selector.omitRegex = regex
            return this
        }

        fun build() = DataMapper(TypeValues(type, selector))

        fun build(sep: String) = DataMapper(TypeValues(type, selector), sep)
    }

    companion object {
        @JvmStatic
        fun <T> simple(type: Class<T>) = DataMapper(TypeValues(type))

        @JvmStatic
        fun <T> simple(type: Class<T>, sep: String) = DataMapper(TypeValues(type), sep)

        @JvmStatic
        fun <T> of(type: Class<T>) = Builder(type)
    }
}