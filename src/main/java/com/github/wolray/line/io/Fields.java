package com.github.wolray.line.io;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ray
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Fields {
    boolean pojo() default false;

    String[] use() default {};

    String[] ignore() default {};

    String regex() default "";
}
