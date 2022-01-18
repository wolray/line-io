package com.github.wolray.line.io

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.util.function.Function

/**
 * @author wolray
 */
class DataMapper<T> @JvmOverloads constructor(
        val typeValues: TypeValues<T>,
        val sep: String = "\u02cc") {
    val joiner: ValuesJoiner<T> by lazy { ValuesJoiner(typeValues) }
    val converter: ValuesConverter.Text<T> by lazy { ValuesConverter.Text(typeValues) }
    val formatter: Function<T, String> by lazy { toFormatter(sep) }
    val parser: Function<String, T> by lazy { toParser(sep) }

    fun newSep(sep: String): DataMapper<T> {
        return if (sep == this.sep) this else DataMapper(typeValues, sep)
    }

    fun toFormatter(sep: String): Function<T, String> {
        return joiner.toFormatter(sep)
    }

    fun toParser(sep: String): Function<String, T> {
        return converter.toParser(sep)
    }

    @JvmOverloads
    fun toWriter(sep: String = this.sep): LineWriter<T> {
        return LineWriter(toFormatter(sep))
    }

    @JvmOverloads
    fun toReader(sep: String = this.sep): CsvReader<T> {
        return CsvReader(converter, sep)
    }

    fun format(t: T): String {
        return formatter.apply(t)
    }

    fun parse(s: String): T {
        return parser.apply(s)
    }

    companion object {
        @JvmStatic
        @Synchronized
        fun scan(clazz: Class<*>) {
            try {
                clazz.declaredFields.asSequence()
                        .filter {
                            Modifier.isStatic(it.modifiers)
                                    && it.type == DataMapper::class.java
                        }
                        .forEach {
                            val typeValues = makeTypeValues(it)
                            it.isAccessible = true
                            if (it[null] == null) {
                                it[null] = DataMapper(typeValues)
                            }
                        }
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException(e)
            }
        }

        private fun makeTypeValues(f: Field): TypeValues<*> {
            val genericType = f.genericType
            if (genericType is ParameterizedType) {
                val argument = genericType.actualTypeArguments[0]
                val type = argument as Class<*>
                val fields = f.getAnnotation(Fields::class.java)
                return fields?.let { TypeValues(type, it) } ?: TypeValues(type)
            }
            throw IllegalStateException("DataMapper type unset")
        }
    }
}
