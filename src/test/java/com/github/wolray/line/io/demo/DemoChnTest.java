package com.github.wolray.line.io.demo;

import com.github.wolray.line.io.DataStream;
import com.github.wolray.line.io.Fields;
import com.github.wolray.line.io.LineReader;
import com.github.wolray.line.io.LineWriter;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author wolray
 */
public class DemoChnTest {
    @Test
    public void demo() {
        // 从本地文件快速获取inputStream（如果文件不存在在返回null
        // 设置CSV的分隔符，以及目标class
        List<Person> persons = LineReader.byCsv(",", Person.class)
            .read("some-path/person.csv")
            // 如果不需要第一行的列名信息，可以直接跳过
            .skipLines(1)
            // 如果文件的列名和class的属性顺序不一致，可以手动设置列名，读取时会自动匹配
            .columns("name", "gender", "age", "weight", "height", "phone")
            // 也可以通过下标的方式设置每个属性对应哪一列
            .columns(0, 1, 2, 4, 6, 8)
            // 也可以通过excel的列名方式来声明
            .excelColumns("A,B,C,E,G,I")
            // 生成一个惰性加载的stream，只有在collect才会真正执行
            .stream()
            // 可以在初始化之后随意计算其他属性
            .peek(p -> p.weightHeightRatio = p.weight / p.height)
            // 过滤年龄大于18的对象
            .filter(p -> p.age >= 18)
            // 把这些对象缓存到另一个csv文件里
            // 下次执行这段代码的时候，如果缓存文件存在则直接从文件里读取
            .cacheCsv("some-path/person_18.csv", Person.class, ",")
            // 转成一个基于单向链表的list，随着数量增加对内存更友好
            .toList();

        // 可以将list直接转成DataStream
        DataStream<Person> stream = DataStream.of(persons)
            // 像普通stream那样限制10个
            .limit(10)
            // 让该stream可以重复使用
            .reuse();

        // 把stream映射为一个名称列表并转为ArrayList
        List<String> names = stream.map(p -> p.name).toArrayList();
        // 生成一个以name为key的set
        Set<String> personSet = stream.toSet(p -> p.name);
        // 生成一个以name为key的map
        Map<String, Person> personMap = stream.toMap(p -> p.name, p -> p);
        // 按照gender分组
        Map<Integer, List<Person>> genderGroupMap = stream.groupBy(p -> p.gender, Collectors.toList());

        // 把persons对象另存到一个csv文件里
        LineWriter.byCsv(",", Person.class)
            .write("some-path/person_dump.csv")
            .autoHeader()
            .asyncWith(persons);
    }

    /**
     * 设置一个临时的DO类，声明public属性以便于使用（推荐方式
     * 将其他的需要二次计算的属性声明为private，就能在转化时自动跳过
     */
    public static class Person {
        public String name;
        public Integer gender;
        public Integer age;
        public Double weight;
        public Double height;
        private Double weightHeightRatio;
    }

    /**
     * 对于更常用的pojo类，打上一个@Fields(pojo = true)的注解即可
     * pojo类同上述推荐的方式恰恰相反，只识别private的字段
     */
    @Fields(pojo = true)
    public static class PojoPerson {
        private String name;
        private Integer gender;
        private Integer age;
        private Double weight;
        private Double height;
        /**
         * 设置transient修饰符用于忽略属性
         */
        private transient Double weightHeightRatio;
    }
}
