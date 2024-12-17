/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import android.content.res.Resources
import com.nextcloud.client.account.AnonymousUser
import com.owncloud.android.datamodel.SharesType
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
            false,
            SharesType.INTERNAL
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
