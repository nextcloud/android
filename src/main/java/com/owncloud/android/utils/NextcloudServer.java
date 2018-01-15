package com.owncloud.android.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines min and max server version. Useful to find not needed code, e.g. if annotated max=12 and last supported 
 * version is 13 the code can be removed.
 */

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface NextcloudServer {
    int min() default -1;

    int max();
}