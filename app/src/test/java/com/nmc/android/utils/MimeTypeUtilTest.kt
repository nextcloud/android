package com.nmc.android.utils

import com.owncloud.android.utils.MimeTypeUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class MimeTypeUtilTest {

    @Test
    fun isJpgOrPngFileTest() {
        assertEquals(true, MimeTypeUtil.isJpgOrPngFile(".jpg"))
        assertEquals(true, MimeTypeUtil.isJpgOrPngFile(".png"))
        assertEquals(true, MimeTypeUtil.isJpgOrPngFile(".jpeg"))

        assertEquals(true, MimeTypeUtil.isJpgOrPngFile("example.jpg"))
        assertEquals(true, MimeTypeUtil.isJpgOrPngFile("example.png"))
        assertEquals(true, MimeTypeUtil.isJpgOrPngFile("example.jpeg"))

        assertEquals(true, MimeTypeUtil.isJpgOrPngFile("example.JPG"))
        assertEquals(true, MimeTypeUtil.isJpgOrPngFile("example.PNG"))
        assertEquals(true, MimeTypeUtil.isJpgOrPngFile("example.JPEG"))

        assertEquals(false, MimeTypeUtil.isJpgOrPngFile(".gif"))
        assertEquals(false, MimeTypeUtil.isJpgOrPngFile("example.gif"))
    }
}