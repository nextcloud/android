package com.owncloud.android.utils

/**
 * Defines min and max server version. Useful to find not needed code, e.g. if annotated max=12 and last supported
 * version is 13 the code can be removed.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class NextcloudServer(
    val min: Int = -1,
    val max: Int
)
