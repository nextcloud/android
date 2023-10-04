package com.owncloud.android.datamodel

import android.content.Context
import android.content.res.Resources
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.common.network.WebdavEntry.MountType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class OCFileIconTests {

    private val path = "/path/to/a/file.txt"
    private var sut: OCFile? = null

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var contextResources: Resources

    @Before
    fun setup() {
        sut = OCFile(path)
        MockitoAnnotations.openMocks(this)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.resources).thenReturn(contextResources)

        MainApp.setAppContext(context)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsAutoUploadFolderShouldReturnFolderOverlayUploadIcon() {
        val fileOverlayIcon = sut?.getFileOverlayIcon(true)
        val expectedDrawable = R.drawable.ic_folder_overlay_upload
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsEncryptedShouldReturnFolderOverlayKeyIcon() {
        sut?.isEncrypted = true
        val fileOverlayIcon = sut?.getFileOverlayIcon(false)
        val expectedDrawable = R.drawable.ic_folder_overlay_key
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsGroupFolderShouldReturnFolderOverlayAccountGroupIcon() {
        sut?.mountType = MountType.GROUP
        val fileOverlayIcon = sut?.getFileOverlayIcon(false)
        val expectedDrawable = R.drawable.ic_folder_overlay_account_group
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsSharedViaLinkShouldReturnFolderOverlayLinkIcon() {
        sut?.isSharedViaLink = true
        val fileOverlayIcon = sut?.getFileOverlayIcon(false)
        val expectedDrawable = R.drawable.ic_folder_overlay_link
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsSharedShouldReturnFolderOverlayShareIcon() {
        sut?.isSharedWithSharee = true
        val fileOverlayIcon = sut?.getFileOverlayIcon(false)
        val expectedDrawable = R.drawable.ic_folder_overlay_share
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsExternalShouldReturnFolderOverlayExternalIcon() {
        sut?.mountType = MountType.EXTERNAL
        val fileOverlayIcon = sut?.getFileOverlayIcon(false)
        val expectedDrawable = R.drawable.ic_folder_overlay_external
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsLockedShouldReturnFolderOverlayLockIcon() {
        sut?.isLocked = true
        val fileOverlayIcon = sut?.getFileOverlayIcon(false)
        val expectedDrawable = R.drawable.ic_folder_overlay_lock
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsFolderShouldReturnNull() {
        val fileOverlayIcon = sut?.getFileOverlayIcon(false)
        assert(fileOverlayIcon == null)
    }

    @After
    fun destroy() {
        sut = null
    }
}