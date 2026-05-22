/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.model

import com.owncloud.android.utils.RichDocumentDownloadAsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RichDocumentDownloadAsParserTests {

    @Test
    fun `parse returns null for null-like blank input`() {
        assertNull(RichDocumentDownloadAsParser.parse(""))
        assertNull(RichDocumentDownloadAsParser.parse("   "))
    }

    @Test
    fun `parse returns null for malformed json`() {
        assertNull(RichDocumentDownloadAsParser.parse("not json at all"))
        assertNull(RichDocumentDownloadAsParser.parse("{invalid}"))
    }

    @Test
    fun `parse returns null when required v2 fields are missing`() {
        assertNull(RichDocumentDownloadAsParser.parse("""{"format":"pdf"}"""))
        assertNull(RichDocumentDownloadAsParser.parse("""{"format":"pdf","name":"file.pdf"}"""))
    }

    @Test
    fun `parse returns null when required v1 fields are missing`() {
        assertNull(RichDocumentDownloadAsParser.parse("""{"Type":"print"}"""))
        assertNull(RichDocumentDownloadAsParser.parse("""{"Type":"print","filename":"file.pdf"}"""))
    }

    @Test
    fun `parse v2 with lowercase url succeeds`() {
        val json = """{"format":"pdf","name":"document.pdf","url":"https://example.com/file.pdf"}"""
        val result = RichDocumentDownloadAsParser.parse(json)
        assertNotNull(result)
        assertEquals("pdf", result!!.format)
        assertEquals("document.pdf", result.fileName)
        assertEquals("https://example.com/file.pdf", result.url)
    }

    @Test
    fun `parse v2 with uppercase URL succeeds`() {
        val json = """{"format":"pdf","name":"document.pdf","URL":"https://example.com/file.pdf"}"""
        val result = RichDocumentDownloadAsParser.parse(json)
        assertNotNull(result)
        assertEquals("pdf", result!!.format)
        assertEquals("document.pdf", result.fileName)
        assertEquals("https://example.com/file.pdf", result.url)
    }

    @Test
    fun `parse v2 with format print succeeds`() {
        val json = """{"format":"print","name":"document.pdf","url":"https://example.com/file.pdf"}"""
        val result = RichDocumentDownloadAsParser.parse(json)
        assertNotNull(result)
        assertEquals("print", result!!.format)
    }

    @Test
    fun `parse v2 with format slideshow succeeds`() {
        val json = """{"format":"slideshow","name":"slides.pdf","url":"https://example.com/slides.pdf"}"""
        val result = RichDocumentDownloadAsParser.parse(json)
        assertNotNull(result)
        assertEquals("slideshow", result!!.format)
    }

    @Test
    fun `parse v1 with uppercase URL succeeds`() {
        val json = """{"Type":"print","URL":"https://example.com/file.pdf","filename":"file.pdf"}"""
        val result = RichDocumentDownloadAsParser.parse(json)
        assertNotNull(result)
        assertEquals("print", result!!.format)
        assertEquals("file.pdf", result.fileName)
        assertEquals("https://example.com/file.pdf", result.url)
    }

    @Test
    fun `parse v1 with lowercase url succeeds`() {
        val json = """{"Type":"print","url":"https://example.com/file.pdf","filename":"file.pdf"}"""
        val result = RichDocumentDownloadAsParser.parse(json)
        assertNotNull(result)
        assertEquals("print", result!!.format)
        assertEquals("file.pdf", result.fileName)
        assertEquals("https://example.com/file.pdf", result.url)
    }

    @Test
    fun `parse v1 with slideshow type succeeds`() {
        val json = """{"Type":"slideshow","URL":"https://example.com/slides.pdf","filename":"slides.pdf"}"""
        val result = RichDocumentDownloadAsParser.parse(json)
        assertNotNull(result)
        assertEquals("slideshow", result!!.format)
        assertEquals("slides.pdf", result.fileName)
    }
}
