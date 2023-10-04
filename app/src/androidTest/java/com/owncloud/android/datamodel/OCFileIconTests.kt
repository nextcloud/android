package com.owncloud.android.datamodel

import android.content.Context
import android.content.res.Resources
import com.owncloud.android.MainApp
import com.owncloud.android.R
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

    @After
    fun destroy() {
        sut = null
    }
}