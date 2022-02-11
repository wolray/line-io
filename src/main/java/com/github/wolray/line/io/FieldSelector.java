package com.github.wolray.line.io;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author wolray
 */
class FieldSelector {
    boolean pojo;
    String[] use;
    String[] omit;
    String useRegex;
    String omitRegex;

    static FieldSelector of(Fields fields) {
        FieldSelector selector = new FieldSelector();
        if (fields != null) {
            selector.pojo = fields.pojo();
            selector.use = fields.use();
            selector.omit = fields.omit();
            selector.useRegex = fields.useRegex();
            selector.omitRegex = fields.omitRegex();
        }
        return selector;
    }

    static Predicate<Field> toPredicate(Fields fields) {
        if (fields == null) {
            return f -> true;
        }
        return of(fields).toPredicate();
    }

    Predicate<Field> toPredicate() {
        if (use != null && use.length > 0) {
            Set<String> set = new HashSet<>(Arrays.asList(use));
            return f -> set.contains(f.getName());
        }
        if (omit != null && omit.length > 0) {
            Set<String> set = new HashSet<>(Arrays.asList(omit));
            return f -> !set.contains(f.getName());
        }
        if (useRegex != null && !useRegex.isEmpty()) {
            return f -> f.getName().matches(useRegex);
        }
        if (omitRegex != null && !omitRegex.isEmpty()) {
            return f -> !f.getName().matches(omitRegex);
        }
        return f -> true;
    }
}
