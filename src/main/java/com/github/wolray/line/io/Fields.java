package com.github.wolray.line.io;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wolray
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Fields {
    boolean pojo() default false;

    String[] use() default {};

    String[] omit() default {};

    String useRegex() default "";

    String omitRegex() default "";
}
