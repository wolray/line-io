package com.github.wolray.line.io.demo

import com.github.wolray.line.io.LineReader
import com.github.wolray.line.io.LineReader.Companion.toInputStream
import com.github.wolray.line.io.enableCache
import com.github.wolray.line.io.toDataList
import org.junit.Test

/**
 * @author wolray
 */
class KotlinTest {
    @Test
    fun demo() {
        val inputStream = toInputStream("some-path/person.csv")
        val persons = LineReader.byCsv(",", DemoTest.Person::class.java)
            .read(inputStream!!)
            .skipLines(1)
            .csvHeader("name", "gender", "age", "weight", "height", "phone")
            .columns(0, 1, 2, 4, 6, 8)
            .columns("A,B,C,E,G,I")
            .sequence()
            .onEach { it.weightHeightRatio = it.weight / it.height }
            .filter { it.age >= 18 }
            .enableCache()
            .cacheCsv("some-path/person_18.csv", DemoTest.Person::class.java, ",")
            .toDataList()
    }
}