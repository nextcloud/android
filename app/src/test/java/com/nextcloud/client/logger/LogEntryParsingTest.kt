/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.logger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class LogEntryParsingTest {

    @Test
    fun parse_validEntry_returnsLogEntry() {
        val input = "1970-01-01T00:00:00.000Z;D;TestTag;Hello World"
        val entry = LogEntry.parse(input)
        assertNotNull(entry)
        assertEquals(Level.DEBUG, entry!!.level)
        assertEquals("TestTag", entry.tag)
        assertEquals("Hello World", entry.message)
    }

    @Test
    fun parse_allLevels_parsedCorrectly() {
        val levels = mapOf(
            "D" to Level.DEBUG,
            "I" to Level.INFO,
            "W" to Level.WARNING,
            "E" to Level.ERROR,
            "A" to Level.ASSERT,
            "V" to Level.VERBOSE
        )
        for ((tag, level) in levels) {
            val input = "2024-06-15T12:30:45.123Z;$tag;tag;msg"
            val entry = LogEntry.parse(input)
            assertNotNull("Failed to parse level $tag", entry)
            assertEquals(level, entry!!.level)
        }
    }

    @Test
    fun parse_messageWithNewlines_decodesCorrectly() {
        val input = "2024-01-01T00:00:00.000Z;I;Tag;line1\\nline2\\nline3"
        val entry = LogEntry.parse(input)
        assertNotNull(entry)
        assertEquals("line1\nline2\nline3", entry!!.message)
    }

    @Test
    fun parse_messageWithSemicolons_preservesSemicolons() {
        val input = "2024-01-01T00:00:00.000Z;D;Tag;key=value;extra=data"
        val entry = LogEntry.parse(input)
        assertNotNull(entry)
        assertEquals("key=value;extra=data", entry!!.message)
    }

    @Test
    fun parse_emptyString_returnsNull() {
        assertNull(LogEntry.parse(""))
    }

    @Test
    fun parse_invalidFormat_returnsNull() {
        assertNull(LogEntry.parse("not a valid log entry"))
        assertNull(LogEntry.parse(";;;;"))
    }

    @Test
    fun parse_invalidDate_returnsNull() {
        assertNull(LogEntry.parse("not-a-date;D;Tag;message"))
    }

    @Test
    fun toString_roundTrip_parsesBack() {
        val original = LogEntry(Date(0), Level.INFO, "TestTag", "Test message")
        val serialized = original.toString()
        val parsed = LogEntry.parse(serialized)
        assertNotNull(parsed)
        assertEquals(original.level, parsed!!.level)
        assertEquals(original.tag, parsed.tag)
        assertEquals(original.message, parsed.message)
    }

    @Test
    fun toString_newlinesInMessage_escapedCorrectly() {
        val entry = LogEntry(Date(0), Level.DEBUG, "Tag", "line1\nline2")
        val serialized = entry.toString()
        assertTrue("Newlines should be escaped", serialized.contains("\\n"))
        assertFalse("Should not contain actual newline", serialized.contains("\n"))
    }

    @Test
    fun toString_semicolonsInTag_replacedWithSpace() {
        val entry = LogEntry(Date(0), Level.DEBUG, "tag;with;semicolons", "msg")
        val serialized = entry.toString()
        assertFalse("Tag semicolons should be replaced", serialized.contains("tag;with"))
    }

    private fun assertTrue(message: String, condition: Boolean) {
        org.junit.Assert.assertTrue(message, condition)
    }

    private fun assertFalse(message: String, condition: Boolean) {
        org.junit.Assert.assertFalse(message, condition)
    }
}
