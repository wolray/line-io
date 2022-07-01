package com.github.wolray.line.io.demo

import com.github.wolray.line.io.CachedSequence
import com.github.wolray.line.io.CsvReader
import com.github.wolray.line.io.enableCache
import com.github.wolray.line.io.toDataList
import org.junit.Test
import java.io.FileNotFoundException

/**
 * @author wolray
 */
class KotlinTest {
    @Test
    fun demo() {
        val list = listOf(1, 2, 3, 4, 5)
        val ls1 = CachedSequence(list).toDataList()
        val ls2 = CachedSequence(list).toList()
        println(list)

        val map = (0..9).associate { it + 10 to it + 100 }
        println(map)
        println(map)

        val persons = CsvReader.of(",", DemoTest.Person::class.java)
            .read("some-path/person.csv")
            .ignoreError(FileNotFoundException::class.java)
            .skipLines(1)
            .columns("name", "gender", "age", "weight", "height", "phone")
            .columns(0, 1, 2, 4, 6, 8)
            .excelColumns("A,B,C,E,G,I")
            .sequence()
            .onEach { it.weightHeightRatio = it.weight / it.height }
            .filter { it.age >= 18 }
            .enableCache()
            .cacheCsv("some-path/person_18.csv", DemoTest.Person::class.java, ",")
            .toDataList()
    }

    @Test
    fun excel() {
        val a = 'A'
        val cols = listOf("A", "B", "Z", "AA", "AB", "AZ", "BA", "AAA")
        cols.forEach {
            println(it.fold(0) { acc, c -> acc * 26 + (c - a + 1) } - 1)
        }
    }
}
