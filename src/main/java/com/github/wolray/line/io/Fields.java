package com.github.wolray.line.io;

import java.lang.annotation.*;

/**
 * @author ray
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Fields {
    boolean pojo() default false;

    String[] use() default {};

    String[] ignore() default {};

    String regex() default "";
}
