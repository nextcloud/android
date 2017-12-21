package com.owncloud.android.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by tobi on 21.12.17.
 */

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface NextcloudServer {
    int min();

    int max();
}