/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.logger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import java.util.Date
import java.util.SimpleTimeZone
import java.util.concurrent.TimeUnit

@RunWith(Suite::class)
@Suite.SuiteClasses(
    LogEntryTest.ToString::class,
    LogEntryTest.Parse::class
)
class LogEntryTest {
    private companion object {
        const val SEVEN_HOUR = 7L
    }

    class ToString {
        @Test
        fun `to string`() {
            val entry = LogEntry(
                timestamp = Date(0),
                level = Level.DEBUG,
                tag = "tag",
                message = "some message"
            )
            assertEquals("1970-01-01T00:00:00.000Z;D;tag;some message", entry.toString())
        }

        @Test
        fun `to string with custom time zone`() {
            val entry = LogEntry(
                timestamp = Date(0),
                level = Level.DEBUG,
                tag = "tag",
                message = "some message"
            )
            val sevenHours = TimeUnit.HOURS.toMillis(SEVEN_HOUR).toInt()
            val tz = SimpleTimeZone(sevenHours, "+0700")
            assertEquals("1970-01-01T07:00:00.000+0700;D;tag;some message", entry.toString(tz))
        }

        @Test
        fun `semicolons are removed from entry tags`() {
            val entry = LogEntry(
                timestamp = Date(0),
                level = Level.DEBUG,
                tag = "t;a;g",
                message = "some message"
            )
            assertEquals("1970-01-01T00:00:00.000Z;D;t a g;some message", entry.toString())
        }

        @Test
        fun `message newline is converted`() {
            val entry = LogEntry(
                timestamp = Date(0),
                level = Level.DEBUG,
                tag = "tag",
                message = "multine\nmessage\n"
            )
            assertTrue(entry.toString().endsWith(";multine\\nmessage\\n"))
        }

        @Test
        fun `tag can contain unicode characters`() {
            val entry = LogEntry(
                timestamp = Date(0),
                level = Level.DEBUG,
                tag =
                """靖康緗素雜記""",
                message = "夏炉冬扇"
            )
            assertEquals("1970-01-01T00:00:00.000Z;D;靖康緗素雜記;夏炉冬扇", entry.toString())
        }
    }

    class Parse {
        @Test
        fun `regexp parser`() {
            val entry = "1970-01-01T00:00:00.000Z;D;tag;some message"
            val parsed = LogEntry.parse(entry)
            assertNotNull(parsed)
            parsed as LogEntry
            assertEquals(Date(0), parsed.timestamp)
            assertEquals(Level.DEBUG, parsed.level)
            assertEquals("tag", parsed.tag)
            assertEquals("some message", parsed.message)
        }

        @Test
        fun `malformed log entries are rejected`() {
            assertNull("no miliseconds", LogEntry.parse("1970-01-01T00:00:00Z;D;tag;a message"))
            assertNull("not zulu", LogEntry.parse("1970-01-01T00:00:00.000+00:00;D;tag;a message"))
            assertNull("not utc", LogEntry.parse("1970-01-01T01:00:00.000+01:00;D;tag;a message"))
            assertNull("bad month", LogEntry.parse("1970-13-01T00:00:00.000Z;D;tag;a message"))
            assertNull("bad year", LogEntry.parse("0000-01-01T00:00:00.000Z;D;tag;a message"))
            assertNull("bad day", LogEntry.parse("1970-01-32T00:00:00.000Z;D;tag;a message"))
            assertNull("bad hour", LogEntry.parse("1970-01-01T25:00:00.000Z;D;tag;a message"))
            assertNull("bad minute", LogEntry.parse("1970-01-01T00:61:00.000Z;D;tag;a message"))
            assertNull("bad second", LogEntry.parse("1970-01-01T00:00:61.000Z;D;tag;a message"))
            assertNull("bad level", LogEntry.parse("1970-01-01T00:00:00.000Z;?;tag;a message"))
            assertNull("empty tag", LogEntry.parse("1970-01-01T00:00:00.000Z;D;;a message"))
            assertNull("empty string", LogEntry.parse(""))
        }

        @Test
        fun `semicolon in tag tears the tag`() {
            val parsed = LogEntry.parse("1970-01-01T00:00:00.000Z;D;t;ag;a message")
            assertNotNull(parsed)
            assertEquals("Tag is cut; no parse error expected", "t", parsed?.tag)
            assertEquals("Tag is cut; no parse error expected", "ag;a message", parsed?.message)
        }

        @Test
        fun `message can have semicolons`() {
            val parsed = LogEntry.parse("1970-01-01T00:00:00.000Z;D;tag;a;message;with;semi;colons")
            assertEquals("a;message;with;semi;colons", parsed?.message)
        }
    }
}
