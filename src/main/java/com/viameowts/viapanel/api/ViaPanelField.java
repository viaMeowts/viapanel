package com.viameowts.viapanel.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public config field as editable through the viaPanel UI.
 *
 * Supported field types: boolean, int, long, float, double, String, enum.
 * Place on public instance fields of the config class passed to
 * {@link ViaPanelProviders#builder}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ViaPanelField {

    /** Display name shown in the panel. Empty = humanized field key. */
    String value() default "";

    /** Russian display name. Empty = {@link #value()} is used for ru too. */
    String valueRu() default "";

    /** Hover description. Empty = no description line. */
    String desc() default "";

    /** Russian hover description. Empty = {@link #desc()} is used for ru too. */
    String descRu() default "";

    /** Section id this field belongs to. Sections are created in order of first appearance. */
    String section() default "general";

    /** Inclusive lower bound for numeric fields. NaN = unbounded. */
    double min() default Double.NaN;

    /** Inclusive upper bound for numeric fields. NaN = unbounded. */
    double max() default Double.NaN;

    /** Sort order inside the section (ascending, ties keep declaration order). */
    int order() default 0;

    /** Mask the current value in the UI (tokens, passwords). Editing still works. */
    boolean secret() default false;
}
