package com.hsf.hotel.it;

import org.junit.jupiter.api.condition.EnabledIf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Skips a PostgreSQL integration test class when neither {@code IT_JDBC_URL} nor a
 * Docker daemon is available (see {@link AbstractPostgresIT}). Put it on every
 * concrete {@code *IT} class: JUnit does not inherit conditions from superclasses.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnabledIf("com.hsf.hotel.it.AbstractPostgresIT#postgresAvailable")
public @interface RequiresPostgres {
}