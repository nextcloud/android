/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.fileactions

import android.webkit.MimeTypeMap
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FileActionTest {

    @Test
    fun currentFolderShowsOnlySafeActionsInMenuOrder() {
        val folder = mock<OCFile>()
        whenever(folder.isFolder).thenReturn(true)
        whenever(folder.mimeType).thenReturn("DIR")
        whenever(folder.remotePath).thenReturn("/folder/")
        whenever(folder.canReshare()).thenReturn(true)
        whenever(folder.canRename()).thenReturn(true)
        whenever(folder.canMove()).thenReturn(true)
        whenever(folder.canWrite()).thenReturn(true)

        val toHide = Mockito.mockStatic(MimeTypeMap::class.java).use { mimeTypeMap ->
            mimeTypeMap.`when`<MimeTypeMap> { MimeTypeMap.getSingleton() }.thenReturn(mock())
            FileAction.getCurrentFolderActionsToHide(folder)
        }
        val shown = FileAction.getActions(setOf(folder)).map { it.id }.filterNot { it in toHide }

        assertEquals(
            listOf(
                R.id.action_favorite,
                R.id.action_unset_favorite,
                R.id.action_see_details,
                R.id.action_send_share_file,
                R.id.action_sync_file,
                R.id.action_encrypted
            ),
            shown
        )
    }

    @Test
    fun favoriteActionsToggleToEachOther() {
        assertEquals(FileAction.UNSET_FAVORITE, FileAction.FAVORITE.toggledFavorite())
        assertEquals(FileAction.FAVORITE, FileAction.UNSET_FAVORITE.toggledFavorite())
        assertNull(FileAction.SEE_DETAILS.toggledFavorite())
    }
}
