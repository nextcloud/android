/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import android.webkit.MimeTypeMap
import com.owncloud.android.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MimeTypeUtilTest {

    companion object {
        private const val ANDROID_MARKDOWN = "text/x-android-markdown"
        private const val ANDROID_ONLY_IMAGE = "image/x-foo"
        private const val GZIP = "application/gzip"
        private const val JSON = "application/json"

        private val androidMimeTypes = mapOf(
            "jpg" to MimeType.JPEG,
            "md" to ANDROID_MARKDOWN,
            "foo" to ANDROID_ONLY_IMAGE
        )
    }

    @Before
    fun setUp() {
        val mimeTypeMap = mockk<MimeTypeMap>()
        every { mimeTypeMap.getMimeTypeFromExtension(any()) } answers { androidMimeTypes[firstArg()] }

        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isImage accepts image mime types regardless of case`() {
        assertTrue(MimeTypeUtil.isImage(MimeType.PNG))
        assertTrue(MimeTypeUtil.isImage("IMAGE/JPEG"))
        assertTrue(MimeTypeUtil.isImage(MimeType.SVG))
    }

    @Test
    fun `isImage rejects djvu images`() {
        assertFalse(MimeTypeUtil.isImage("image/vnd.djvu"))
        assertFalse(MimeTypeUtil.isImage("IMAGE/X-DJVU"))
    }

    @Test
    fun `isImage rejects other types, missing slash and null`() {
        assertFalse(MimeTypeUtil.isImage(MimeType.MP4))
        assertFalse(MimeTypeUtil.isImage("image"))
        assertFalse(MimeTypeUtil.isImage(""))
        assertFalse(MimeTypeUtil.isImage(null))
    }

    @Test
    fun `isVideo accepts video mime types regardless of case`() {
        assertTrue(MimeTypeUtil.isVideo(MimeType.MP4))
        assertTrue(MimeTypeUtil.isVideo("VIDEO/QUICKTIME"))
    }

    @Test
    fun `isVideo rejects other types and null`() {
        assertFalse(MimeTypeUtil.isVideo(MimeType.JPEG))
        assertFalse(MimeTypeUtil.isVideo("audio/mpeg"))
        assertFalse(MimeTypeUtil.isVideo(null))
    }

    @Test
    fun `isAudio accepts audio mime types regardless of case`() {
        assertTrue(MimeTypeUtil.isAudio("audio/mpeg"))
        assertTrue(MimeTypeUtil.isAudio("AUDIO/OGG"))
    }

    @Test
    fun `isAudio rejects other types and null`() {
        assertFalse(MimeTypeUtil.isAudio(MimeType.MP4))
        assertFalse(MimeTypeUtil.isAudio(null))
    }

    @Test
    fun `isText accepts text mime types regardless of case`() {
        assertTrue(MimeTypeUtil.isText(MimeType.TEXT_PLAIN))
        assertTrue(MimeTypeUtil.isText(MimeType.TEXT_MARKDOWN))
        assertTrue(MimeTypeUtil.isText("TEXT/X-SHELLSCRIPT"))
    }

    @Test
    fun `isText rejects application dependent documents and null`() {
        assertFalse(MimeTypeUtil.isText("application/msword"))
        assertFalse(MimeTypeUtil.isText("application/json"))
        assertFalse(MimeTypeUtil.isText(null))
    }

    @Test
    fun `isMedia accepts images, videos and audio`() {
        assertTrue(MimeTypeUtil.isMedia(MimeType.JPEG))
        assertTrue(MimeTypeUtil.isMedia(MimeType.MP4))
        assertTrue(MimeTypeUtil.isMedia("audio/mpeg"))
    }

    @Test
    fun `isMedia rejects text, djvu and null`() {
        assertFalse(MimeTypeUtil.isMedia(MimeType.TEXT_PLAIN))
        assertFalse(MimeTypeUtil.isMedia("image/vnd.djvu"))
        assertFalse(MimeTypeUtil.isMedia(null))
    }

    @Test
    fun `isImageOrVideo accepts images and videos only`() {
        assertTrue(MimeTypeUtil.isImageOrVideo(MimeType.WEBP))
        assertTrue(MimeTypeUtil.isImageOrVideo(MimeType.MP4))
        assertFalse(MimeTypeUtil.isImageOrVideo("audio/mpeg"))
        assertFalse(MimeTypeUtil.isImageOrVideo(null))
    }

    @Test
    fun `isVCard matches the vcard mime type exactly, ignoring case`() {
        assertTrue(MimeTypeUtil.isVCard(MimeType.VCARD))
        assertTrue(MimeTypeUtil.isVCard("TEXT/VCARD"))
        assertFalse(MimeTypeUtil.isVCard("text/vcard; charset=utf-8"))
        assertFalse(MimeTypeUtil.isVCard(null))
    }

    @Test
    fun `isCalendar matches the calendar mime type exactly, ignoring case`() {
        assertTrue(MimeTypeUtil.isCalendar(MimeType.CALENDAR))
        assertTrue(MimeTypeUtil.isCalendar("Text/Calendar"))
        assertFalse(MimeTypeUtil.isCalendar(MimeType.TEXT_PLAIN))
        assertFalse(MimeTypeUtil.isCalendar(null))
    }

    @Test
    fun `isPDF matches the pdf mime type exactly, ignoring case`() {
        assertTrue(MimeTypeUtil.isPDF(MimeType.PDF))
        assertTrue(MimeTypeUtil.isPDF("APPLICATION/PDF"))
        assertFalse(MimeTypeUtil.isPDF("application/x-pdf"))
        assertFalse(MimeTypeUtil.isPDF(null))
    }

    @Test
    fun `isSVG matches the svg mime type exactly, ignoring case`() {
        assertTrue(MimeTypeUtil.isSVG(MimeType.SVG))
        assertTrue(MimeTypeUtil.isSVG("IMAGE/SVG+XML"))
        assertFalse(MimeTypeUtil.isSVG(MimeType.PNG))
        assertFalse(MimeTypeUtil.isSVG(null))
    }

    @Test
    fun `isFolder matches only the directory mime type, ignoring case`() {
        assertTrue(MimeTypeUtil.isFolder(MimeType.DIRECTORY))
        assertTrue(MimeTypeUtil.isFolder("dir"))
        assertFalse(MimeTypeUtil.isFolder(MimeType.WEBDAV_FOLDER))
        assertFalse(MimeTypeUtil.isFolder(null))
    }

    @Test
    fun `getMimeTypeFromPath prefers the Android mime type over the Nextcloud mapping`() {
        assertEquals(ANDROID_MARKDOWN, MimeTypeUtil.getMimeTypeFromPath("/docs/notes.md"))
    }

    @Test
    fun `getMimeTypeFromPath falls back to the Nextcloud mapping for extensions unknown to Android`() {
        assertEquals(GZIP, MimeTypeUtil.getMimeTypeFromPath("/backup/archive.gz"))
    }

    @Test
    fun `getMimeTypeFromPath picks the first Nextcloud candidate`() {
        assertEquals(JSON, MimeTypeUtil.getMimeTypeFromPath("/config.json"))
    }

    @Test
    fun `getMimeTypeFromPath ignores the case of the extension`() {
        assertEquals(MimeType.JPEG, MimeTypeUtil.getMimeTypeFromPath("/camera/PHOTO.JPG"))
    }

    @Test
    fun `getMimeTypeFromPath uses the last extension of a multi dot name`() {
        assertEquals(GZIP, MimeTypeUtil.getMimeTypeFromPath("/backup/site.tar.gz"))
    }

    @Test
    fun `getMimeTypeFromPath returns octet-stream for unknown extensions`() {
        assertEquals(MimeType.FILE, MimeTypeUtil.getMimeTypeFromPath("/data.xyz"))
    }

    @Test
    fun `getMimeTypeFromPath returns octet-stream for paths without extension`() {
        assertEquals(MimeType.FILE, MimeTypeUtil.getMimeTypeFromPath("/docs/README"))
        assertEquals(MimeType.FILE, MimeTypeUtil.getMimeTypeFromPath(""))
    }

    @Test
    fun `getMimeTypeFromPath treats a dot in a parent folder name as the extension`() {
        assertEquals(MimeType.FILE, MimeTypeUtil.getMimeTypeFromPath("/release.v2/README"))
    }

    @Test
    fun `getBestMimeTypeByFilename prefers the Nextcloud mapping over Android`() {
        assertEquals(MimeType.TEXT_MARKDOWN, MimeTypeUtil.getBestMimeTypeByFilename("notes.md"))
    }

    @Test
    fun `getBestMimeTypeByFilename falls back to Android for extensions unknown to Nextcloud`() {
        assertEquals(ANDROID_ONLY_IMAGE, MimeTypeUtil.getBestMimeTypeByFilename("picture.foo"))
    }

    @Test
    fun `getBestMimeTypeByFilename picks the first Nextcloud candidate`() {
        assertEquals(JSON, MimeTypeUtil.getBestMimeTypeByFilename("config.json"))
    }

    @Test
    fun `getBestMimeTypeByFilename ignores the case of the extension`() {
        assertEquals(GZIP, MimeTypeUtil.getBestMimeTypeByFilename("ARCHIVE.GZ"))
    }

    @Test
    fun `getBestMimeTypeByFilename returns octet-stream for unknown extensions`() {
        assertEquals(MimeType.FILE, MimeTypeUtil.getBestMimeTypeByFilename("data.xyz"))
        assertEquals(MimeType.FILE, MimeTypeUtil.getBestMimeTypeByFilename("README"))
    }

    @Test
    fun `getBestMimeTypeByFilename treats a name without dot as the extension`() {
        assertEquals(GZIP, MimeTypeUtil.getBestMimeTypeByFilename("gz"))
    }

    @Test
    fun `getFileTypeIconId uses the given mime type instead of the filename`() {
        assertEquals(R.drawable.file_pdf, MimeTypeUtil.getFileTypeIconId(MimeType.PDF, "notes.txt"))
    }

    @Test
    fun `getFileTypeIconId resolves exact mime type mappings`() {
        assertEquals(R.drawable.file_zip, MimeTypeUtil.getFileTypeIconId("application/zip", ""))
        assertEquals(R.drawable.file_vcard, MimeTypeUtil.getFileTypeIconId(MimeType.VCARD, ""))
        assertEquals(R.drawable.file_calendar, MimeTypeUtil.getFileTypeIconId(MimeType.CALENDAR, ""))
        assertEquals(R.drawable.folder, MimeTypeUtil.getFileTypeIconId(MimeType.DIRECTORY, ""))
    }

    @Test
    fun `getFileTypeIconId falls back to the main part of an unmapped mime type`() {
        assertEquals(R.drawable.file_image, MimeTypeUtil.getFileTypeIconId("image/x-unknown", ""))
        assertEquals(R.drawable.file_movie, MimeTypeUtil.getFileTypeIconId("video/x-unknown", ""))
        assertEquals(R.drawable.file_sound, MimeTypeUtil.getFileTypeIconId("audio/x-unknown", ""))
        assertEquals(R.drawable.file_text, MimeTypeUtil.getFileTypeIconId("text/x-unknown", ""))
    }

    @Test
    fun `getFileTypeIconId returns the generic file icon for unknown mime types`() {
        assertEquals(R.drawable.file, MimeTypeUtil.getFileTypeIconId("application/x-unknown", ""))
        assertEquals(R.drawable.file, MimeTypeUtil.getFileTypeIconId(MimeType.FILE, ""))
    }

    @Test
    fun `getFileTypeIconId returns the generic file icon for a mime type without main part`() {
        assertEquals(R.drawable.file, MimeTypeUtil.getFileTypeIconId("/", ""))
        assertEquals(R.drawable.file, MimeTypeUtil.getFileTypeIconId("", ""))
    }

    @Test
    fun `getFileTypeIconId resolves the extension when no mime type is given`() {
        assertEquals(R.drawable.file_pdf, MimeTypeUtil.getFileTypeIconId(null, "report.pdf"))
        assertEquals(R.drawable.file_code, MimeTypeUtil.getFileTypeIconId(null, "config.json"))
        assertEquals(R.drawable.file_zip, MimeTypeUtil.getFileTypeIconId(null, "ARCHIVE.ZIP"))
    }

    @Test
    fun `getFileTypeIconId uses the main type of an Android only extension`() {
        assertEquals(R.drawable.file_image, MimeTypeUtil.getFileTypeIconId(null, "picture.foo"))
    }

    @Test
    fun `getFileTypeIconId returns the generic file icon for unknown extensions`() {
        assertEquals(R.drawable.file, MimeTypeUtil.getFileTypeIconId(null, "data.xyz"))
        assertEquals(R.drawable.file, MimeTypeUtil.getFileTypeIconId(null, "README"))
    }
}
