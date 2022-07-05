package com.github.wolray.line.io

import com.github.wolray.line.io.MethodScope.annotation
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * @author wolray
 */
class TypeValues<T> @JvmOverloads constructor(
    val type: Class<T>,
    selector: FieldSelector = FieldSelector.of(type.annotation())
) : Iterable<Field> {
    val values: List<Field> = getFields(type, selector)
    val size: Int = values.size

    private fun getFields(type: Class<T>, selector: FieldSelector): List<Field> {
        val seq = if (selector.pojo) {
            type.declaredFields.asSequence()
                .filter { Modifier.isPrivate(it.modifiers) }
                .onEach { it.isAccessible = true }
        } else {
            type.fields.asSequence()
        }
        val test = selector.toTest()
        return seq.filter { test(it) && it.modifiers and sft == 0 }.toList()
    }

    override fun iterator(): Iterator<Field> = values.iterator()

    class SimpleMethod(val method: Method, val paraType: Class<*>, val returnType: Class<*>)

    companion object {
        const val sft = Modifier.STATIC or Modifier.FINAL or Modifier.TRANSIENT

        fun processSimpleMethods(type: Class<*>, consumer: (SimpleMethod) -> Unit) {
            for (m in type.declaredMethods) {
                if (Modifier.isStatic(m.modifiers)) {
                    val parameterTypes = m.parameterTypes
                    val returnType = m.returnType
                    if (parameterTypes.size == 1 && returnType != Void.TYPE) {
                        consumer(SimpleMethod(m, parameterTypes[0], returnType))
                    }
                }
            }
        }
    }
}
