/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2021 Álvaro Brey Vilas
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class ContentResolverHelperTest {

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
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
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
