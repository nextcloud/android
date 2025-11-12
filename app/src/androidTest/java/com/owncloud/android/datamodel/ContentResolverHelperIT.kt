/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ContentResolverHelperIT {

    companion object {
        private val URI = Uri.parse("http://foo.bar")
        private val PROJECTION = arrayOf("Foo")
        private const val SELECTION = "selection"
        private const val SORT_COLUMN = "sortColumn"
        private const val SORT_DIRECTION = ContentResolverHelper.SORT_DIRECTION_ASCENDING
        private const val SORT_DIRECTION_INT = ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
        private const val LIMIT = 10
    }

    @Mock
    lateinit var resolver: ContentResolver

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun contentResolver_onAndroid26_usesNewAPI() {
        ContentResolverHelper
            .queryResolver(resolver, URI, PROJECTION, SELECTION, null, SORT_COLUMN, SORT_DIRECTION, LIMIT)

        verify(resolver).query(
            eq(URI),
            eq(PROJECTION),
            argThat { bundle ->
                bundle.getString(ContentResolver.QUERY_ARG_SQL_SELECTION) == SELECTION &&
                    bundle.getInt(ContentResolver.QUERY_ARG_LIMIT) == LIMIT &&
                    bundle.getStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS)!!
                        .contentEquals(arrayOf(SORT_COLUMN)) &&
                    bundle.getInt(ContentResolver.QUERY_ARG_SORT_DIRECTION) == SORT_DIRECTION_INT
            },
            null
        )
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    fun contentResolver_onAndroidBelow26_usesOldAPI() {
        ContentResolverHelper
            .queryResolver(resolver, URI, PROJECTION, SELECTION, null, SORT_COLUMN, SORT_DIRECTION, LIMIT)

        verify(resolver).query(
            eq(URI),
            eq(PROJECTION),
            eq(SELECTION),
            eq(null),
            eq("$SORT_COLUMN $SORT_DIRECTION LIMIT $LIMIT"),
            eq(null)
        )
    }
}
