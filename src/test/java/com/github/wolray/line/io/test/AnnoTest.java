package com.github.wolray.line.io.test;

import com.github.wolray.line.io.CsvWriter;
import com.github.wolray.line.io.LineReader;
import lombok.ToString;
import org.junit.Test;

import java.util.List;

/**
 * @author wolray
 */
public class AnnoTest {
    @Test
    public void test() {
        List<Person> list = LineReader.byExcel(Person.class)
            .read(getClass(), "/line.xlsx")
            .columns("姓名", "年龄", "体重")
            .stream()
            .toList();
        list.forEach(System.out::println);
        CsvWriter.of(",", Person.class)
            .write("src/test/resources/line.csv")
            .autoHeader()
            .with(list);
    }

    @ToString
    public static class Person {
        public String name;
        public Integer age;
        public Double weight;
    }
}
