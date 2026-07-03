/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.model

import com.owncloud.android.utils.RichDocumentDownloadAsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RichDocumentDownloadAsParserExtensionTests {

    @Test
    fun `parse v2 appends pdf extension when name has none`() {
        val json = """{"format":"pdf","name":"document","url":"https://example.com/token"}"""
        val result = RichDocumentDownloadAsParser.parse(json)
        assertNotNull(result)
        assertEquals("document.pdf", result!!.filename)
    }

    @Test
    fun `parse v2 appends epub extension when name has none`() {
        val json = """{"format":"epub","name":"book","url":"https://example.com/token"}"""
        val result = RichDocumentDownloadAsParser.parse(json)
        assertNotNull(result)
        assertEquals("book.epub", result!!.filename)
    }

    @Test
    fun `parse v2 keeps existing extension`() {
        val json = """{"format":"pdf","name":"document.pdf","url":"https://example.com/token"}"""
        val result = RichDocumentDownloadAsParser.parse(json)
        assertNotNull(result)
        assertEquals("document.pdf", result!!.filename)
    }

    @Test
    fun `parse v1 export without filename produces extensionless fallback`() {
        val json = """{"Type":"export","URL":"https://example.com/download/token?WOPISrc=x"}"""
        val result = RichDocumentDownloadAsParser.parse(json)
        assertNotNull(result)
        assertEquals("export", result!!.format)
        assertFalse(RichDocumentDownloadAsParser.hasExtension(result.filename))
    }

    @Test
    fun `hasExtension detects extension presence`() {
        assertTrue(RichDocumentDownloadAsParser.hasExtension("document.pdf"))
        assertFalse(RichDocumentDownloadAsParser.hasExtension("document"))
        assertFalse(RichDocumentDownloadAsParser.hasExtension(".pdf"))
        assertFalse(RichDocumentDownloadAsParser.hasExtension("document."))
    }

    @Test
    fun `filenameFromContentDisposition parses quoted filename`() {
        val header = """attachment; filename="MyDocument.pdf""""
        assertEquals("MyDocument.pdf", RichDocumentDownloadAsParser.filenameFromContentDisposition(header))
    }

    @Test
    fun `filenameFromContentDisposition parses unquoted filename`() {
        val header = "attachment; filename=MyDocument.epub"
        assertEquals("MyDocument.epub", RichDocumentDownloadAsParser.filenameFromContentDisposition(header))
    }

    @Test
    fun `filenameFromContentDisposition parses rfc5987 encoded filename`() {
        val header = "attachment; filename*=UTF-8''My%20Document.pdf"
        assertEquals("My Document.pdf", RichDocumentDownloadAsParser.filenameFromContentDisposition(header))
    }

    @Test
    fun `filenameFromContentDisposition returns null for blank or missing`() {
        assertNull(RichDocumentDownloadAsParser.filenameFromContentDisposition(null))
        assertNull(RichDocumentDownloadAsParser.filenameFromContentDisposition(""))
        assertNull(RichDocumentDownloadAsParser.filenameFromContentDisposition("attachment"))
    }
}
