/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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
package com.owncloud.android.ui.adapter

import android.content.Context
import android.content.res.Resources
import com.nextcloud.client.account.AnonymousUser
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.theme.ViewThemeUtils
import org.junit.Assert
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class ShareeListAdapterTest {
    @Mock
    private val context: Context? = null

    @Mock
    private val fileActivity: FileActivity? = null

    @Mock
    private lateinit var viewThemeUtils: ViewThemeUtils

    private val orderedShares = listOf(
        OCShare("/1").apply {
            shareType = ShareType.EMAIL
            sharedDate = 1004
        },
        OCShare("/2").apply {
            shareType = ShareType.PUBLIC_LINK
            sharedDate = 1003
        },
        OCShare("/3").apply {
            shareType = ShareType.PUBLIC_LINK
            sharedDate = 1001
        },
        OCShare("/4").apply {
            shareType = ShareType.EMAIL
            sharedDate = 1000
        },
        OCShare("/5").apply {
            shareType = ShareType.USER
            sharedDate = 80
        },
        OCShare("/6").apply {
            shareType = ShareType.CIRCLE
            sharedDate = 20
        }
    )

    @Test
    fun testSorting() {
        MockitoAnnotations.openMocks(this)
        val resources = Mockito.mock(Resources::class.java)
        Mockito.`when`(context!!.resources).thenReturn(resources)
        Mockito.`when`(fileActivity!!.resources).thenReturn(resources)

        val randomOrder = orderedShares.shuffled()
        val user = AnonymousUser("nextcloud")

        val sut = ShareeListAdapter(
            fileActivity,
            randomOrder,
            null,
            user.accountName,
            user,
            viewThemeUtils,
            false
        )
        sut.sortShares()

        // compare
        assertSort(sut.shares)
    }

    private fun assertSort(shares: MutableList<OCShare>) {
        var compare = true
        var i = 0
        while (i < orderedShares.size && compare) {
            compare = orderedShares[i] === shares[i]
            i++
        }
        if (!compare) {
            println("Expected:")
            for (item in orderedShares) {
                println(item.path + " " + item.shareType + " " + item.sharedDate)
            }
            println()
            println("Actual:")
            for (item in shares) {
                println(item.path + " " + item.shareType + " " + item.sharedDate)
            }
        }
        Assert.assertTrue(compare)
    }
}
