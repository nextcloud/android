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

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class LogEntry(val timestamp: Date, val level: Level, val tag: String, val message: String) {

    companion object {
        private const val UTC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        private const val TZ_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        private val TIME_ZONE = TimeZone.getTimeZone("UTC")
        private val DATE_GROUP_INDEX = 1
        private val LEVEL_GROUP_INDEX = 2
        private val TAG_GROUP_INDEX = 3
        private val MESSAGE_GROUP_INDEX = 4

        /**
         *  <iso8601 date>;<level tag>;<entry tag>;<message>
         *  1970-01-01T00:00:00.000Z;D;tag;some message
         */
        private val ENTRY_PARSE_REGEXP = Regex(
            pattern =
            """(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z);([ADEIVW]);([^;]+);(.*)"""
        )

        @JvmStatic
        fun buildDateFormat(tz: TimeZone? = null): SimpleDateFormat {
            return if (tz == null) {
                SimpleDateFormat(UTC_DATE_FORMAT, Locale.US).apply {
                    timeZone = TIME_ZONE
                    isLenient = false
                }
            } else {
                SimpleDateFormat(TZ_DATE_FORMAT, Locale.US).apply {
                    timeZone = tz
                    isLenient = false
                }
            }
        }

        @Suppress("ReturnCount")
        @JvmStatic
        fun parse(s: String): LogEntry? {
            val result = ENTRY_PARSE_REGEXP.matchEntire(s) ?: return null

            val date = try {
                buildDateFormat().parse(result.groupValues[DATE_GROUP_INDEX])
            } catch (ex: ParseException) {
                return null
            }

            val level: Level = Level.fromTag(result.groupValues[LEVEL_GROUP_INDEX])
            val tag = result.groupValues[TAG_GROUP_INDEX]
            val message = result.groupValues[MESSAGE_GROUP_INDEX].replace("\\n", "\n")

            return LogEntry(
                timestamp = date,
                level = level,
                tag = tag,
                message = message
            )
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        format(sb, buildDateFormat())
        return sb.toString()
    }

    fun toString(tz: TimeZone): String {
        val sb = StringBuilder()
        format(sb, buildDateFormat(tz))
        return sb.toString()
    }

    private fun format(sb: StringBuilder, dateFormat: SimpleDateFormat) {
        sb.append(dateFormat.format(timestamp))
        sb.append(';')
        sb.append(level.tag)
        sb.append(';')
        sb.append(tag.replace(';', ' '))
        sb.append(';')
        sb.append(message.replace("\n", "\\n"))
    }
}
