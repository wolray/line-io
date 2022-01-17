package com.github.wolray.line.io

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.util.function.Function

/**
 * @author wolray
 */
class DataMapper<T> @JvmOverloads constructor(val typeValues: TypeValues<T>, val sep: String = "\u02cc") {
    private var joiner: ValuesJoiner<T>? = null
    private var converter: ValuesConverter.Text<T>? = null
    private var formatter: Function<T, String>? = null
    private var parser: Function<String, T>? = null

    fun newSep(sep: String): DataMapper<T> {
        return if (sep == this.sep) this else DataMapper(typeValues, sep)
    }

    fun getJoiner(): ValuesJoiner<T> {
        if (joiner == null) {
            joiner = ValuesJoiner(typeValues)
        }
        return joiner!!
    }

    fun getConverter(): ValuesConverter.Text<T> {
        if (converter == null) {
            converter = ValuesConverter.Text(typeValues)
        }
        return converter!!
    }

    fun toFormatter(sep: String): Function<T, String> {
        return getJoiner().toFormatter(sep)
    }

    fun toParser(sep: String): Function<String, T> {
        return getConverter().toParser(sep)
    }

    @JvmOverloads
    fun toWriter(sep: String = this.sep): LineWriter<T> {
        return LineWriter(toFormatter(sep))
    }

    @JvmOverloads
    fun toReader(sep: String = this.sep): CsvReader<T> {
        return CsvReader(getConverter(), sep)
    }

    fun format(t: T): String {
        return try {
            formatter!!.apply(t)
        } catch (e: NullPointerException) {
            if (formatter != null) {
                throw e
            }
            formatter = toFormatter(sep)
            formatter!!.apply(t)
        }
    }

    fun parse(s: String): T {
        return try {
            parser!!.apply(s)
        } catch (e: NullPointerException) {
            if (parser != null) {
                throw e
            }
            parser = toParser(sep)
            parser!!.apply(s)
        }
    }

    companion object {
        @JvmStatic
        @Synchronized
        fun scan(clazz: Class<*>) {
            try {
                clazz.declaredFields
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
