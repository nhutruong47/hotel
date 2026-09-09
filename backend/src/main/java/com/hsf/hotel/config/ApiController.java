package com.hsf.hotel.config;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Required marker for JSON and binary HTTP APIs.
 *
 * <p>It enables bean validation and gives architecture tests a single marker
 * to enforce on every current and future controller.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController
@Validated
public @interface ApiController {
}
