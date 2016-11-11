package com.icegreen.greenmail.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * See GreenMailRuleWithStoreChooser for more Javadoc about how to use the StoreChooser annotation in Junit tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface StoreChooser {
    String store();
}