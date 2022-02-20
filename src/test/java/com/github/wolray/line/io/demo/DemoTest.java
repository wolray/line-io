package com.github.wolray.line.io.demo;

import com.github.wolray.line.io.DataStream;
import com.github.wolray.line.io.Fields;
import com.github.wolray.line.io.LineReader;
import com.github.wolray.line.io.LineWriter;
import lombok.Getter;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author wolray
 */
public class DemoTest {
    @Test
    public void demo() {
        // a quick util to get inputStream from a local file (null if the file doesn't exists
        InputStream inputStream = LineReader.toInputStream("some-path/person.csv");
        // set csv separator and target object class
        List<Person> persons = LineReader.byCsv(",", Person.class)
            .read(inputStream)
            // skip 1 line if you don't need the header
            .skipLines(1)
            // if you need specify the header (file columns doesn't align with the class fields)
            .csvHeader("name", "gender", "age", "weight", "height", "phone")
            // or you may like to specify the columns by indices
            .columns(0, 1, 2, 4, 6, 8)
            // or using excel-style columns
            .columns("A,B,C,E,G,I")
            // generate a lazy stream that will not be evaluated until collecting.
            .stream()
            // computing properties right after converting the object
            .peek(p -> p.weightHeightRatio = p.weight / p.height)
            // filter 18+ aged persons
            .filter(p -> p.age >= 18)
            // cache these persons into another csv file
            // next time when running the code the stream will be collected from the cached file
            .cacheCsv("some-path/person_18.csv", Person.class, ",")
            // collect a singly-linked list rather than an ArrayList or LinkedList for better memory efficiency
            .toList();

        // get a stream directly from a list
        DataStream<Person> stream = DataStream.of(persons)
            // limit 10 instances
            .limit(10)
            // make this stream reusable
            .reuse();

        // map to a name stream and collect to an array list
        List<String> names = stream.map(p -> p.name).toArrayList();
        System.out.println(names);
        // generate a person set with name as the key
        Set<String> personSet = stream.toSet(p -> p.name);
        System.out.println(personSet);
        // generate a person map with name as the key
        Map<String, Person> personMap = stream.toMap(p -> p.name, p -> p);
        System.out.println(personMap);
        // groupby persons by their gender
        Map<Integer, List<Person>> genderGroupMap = stream.groupBy(p -> p.gender, Collectors.toList());
        System.out.println(genderGroupMap);

        // write persons to another csv file
        LineWriter.byCsv(",", Person.class)
            .write("some-path/person_dump.csv")
            .autoHeader()
            .asyncWith(persons);
    }

    /**
     * define a temp DO class with public fields, which is easy to access than pojo (recommended way)
     * define extra private fields that is omitted during the conversion and could be computed afterwards
     */
    public static class Person {
        public String name;
        public Integer gender;
        public Integer age;
        public Double weight;
        public Double height;
        public Double weightHeightRatio;
    }

    /**
     * adding a @Fields annotation and set pojo = true if it is a pojo (only private fields are considered)
     */
    @Fields(pojo = true)
    public static class PojoPerson {
        private String name;
        private Integer gender;
        private Integer age;
        private Double weight;
        private Double height;
        /**
         * set transient to omit the field
         */
        private transient Double weightHeightRatio;
    }
}
