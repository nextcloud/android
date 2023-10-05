/*
 * Nextcloud Android Library is available under MIT license
 * @author Alper Öztürk
 * Copyright (C) 2023 Alper Öztürk
 * Copyright (C) 2023 Nextcloud GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.owncloud.android.datamodel

import com.owncloud.android.R
import com.owncloud.android.lib.common.network.WebdavEntry.MountType
import org.junit.After
import org.junit.Before
import org.junit.Test

class OCFileIconTests {

    private val path = "/path/to/a/file.txt"
    private var sut: OCFile? = null

    @Before
    fun setup() {
        sut = OCFile(path)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsAutoUploadFolderShouldReturnFolderOverlayUploadIcon() {
        val fileOverlayIcon = sut?.getFileOverlayIconId(true)
        val expectedDrawable = R.drawable.ic_folder_overlay_upload
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsEncryptedShouldReturnFolderOverlayKeyIcon() {
        sut?.isEncrypted = true
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.ic_folder_overlay_key
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsGroupFolderShouldReturnFolderOverlayAccountGroupIcon() {
        sut?.mountType = MountType.GROUP
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.ic_folder_overlay_account_group
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsSharedViaLinkShouldReturnFolderOverlayLinkIcon() {
        sut?.isSharedViaLink = true
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.ic_folder_overlay_link
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsSharedShouldReturnFolderOverlayShareIcon() {
        sut?.isSharedWithSharee = true
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.ic_folder_overlay_share
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsExternalShouldReturnFolderOverlayExternalIcon() {
        sut?.mountType = MountType.EXTERNAL
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.ic_folder_overlay_external
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsLockedShouldReturnFolderOverlayLockIcon() {
        sut?.isLocked = true
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.ic_folder_overlay_lock
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsFolderShouldReturnNull() {
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        assert(fileOverlayIcon == null)
    }

    @After
    fun destroy() {
        sut = null
    }

}
