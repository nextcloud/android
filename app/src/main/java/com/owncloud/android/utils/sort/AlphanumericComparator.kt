/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils.sort

import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import java.text.Collator

/**
 * Sorts names the way people read them, so `abc2` comes before `abc10` instead of after it.
 *
 */
class AlphanumericComparator<T : Any> : Comparator<T> {

    override fun compare(first: T, second: T): Int = compare(first.toString(), second.toString())

    companion object {

        @JvmStatic
        fun compare(first: ServerFileInterface, second: ServerFileInterface): Int =
            compare(first.fileName, second.fileName)

        @JvmStatic
        fun compare(first: String, second: String): Int = if (first == second) 0 else compareByChunks(first, second)

        private fun compareByChunks(first: String, second: String): Int {
            var ourStart = 0
            var theirStart = 0

            while (ourStart < first.length && theirStart < second.length) {
                val ours = chunkAt(first, ourStart)
                val theirs = chunkAt(second, theirStart)

                val byChunk = compareChunks(ours, theirs)
                if (byChunk != 0) {
                    return byChunk
                }

                ourStart += ours.length
                theirStart += theirs.length
            }

            return when (val byLength = first.length.compareTo(second.length)) {
                0 -> first.compareTo(second)
                else -> byLength
            }
        }

        private val collators: ThreadLocal<Collator> = ThreadLocal.withInitial { Collator.getInstance() }

        private val collator: Collator
            get() = collators.get() ?: Collator.getInstance()

        private fun chunkAt(name: String, start: Int): String {
            val kind = kindOf(name[start])
            var end = start + 1

            if (kind != ChunkKind.SEPARATOR) {
                while (end < name.length && kindOf(name[end]) == kind) {
                    end++
                }
            }

            return name.substring(start, end)
        }

        private fun compareChunks(ours: String, theirs: String): Int {
            val kind = kindOf(ours[0])
            val byKind = kind.compareTo(kindOf(theirs[0]))
            if (byKind != 0) {
                return byKind
            }

            return when (kind) {
                ChunkKind.SEPARATOR -> compareSeparators(ours[0], theirs[0])
                ChunkKind.NUMBER -> compareNumbers(ours, theirs)
                ChunkKind.TEXT -> compareText(ours, theirs)
            }
        }

        private fun kindOf(char: Char): ChunkKind = when {
            char <= LAST_ASCII_PUNCTUATION && !char.isLetterOrDigit() -> ChunkKind.SEPARATOR
            char in ZERO..NINE -> ChunkKind.NUMBER
            else -> ChunkKind.TEXT
        }

        private fun compareSeparators(ours: Char, theirs: Char): Int = when {
            ours == theirs -> 0
            ours == DOT -> -1
            theirs == DOT -> 1
            else -> ours.compareTo(theirs)
        }

        private fun compareNumbers(ours: String, theirs: String): Int {
            val ourValue = ours.trimStart(ZERO)
            val theirValue = theirs.trimStart(ZERO)

            val byDigitCount = ourValue.length.compareTo(theirValue.length)
            if (byDigitCount != 0) {
                return byDigitCount
            }

            return when (val byValue = ourValue.compareTo(theirValue)) {
                0 -> ours.length.compareTo(theirs.length)
                else -> byValue
            }
        }

        private fun compareText(ours: String, theirs: String): Int {
            val byCollation = collator.compare(ours, theirs)
            if (byCollation != 0) {
                return byCollation
            }

            return when (val byLength = ours.length.compareTo(theirs.length)) {
                0 -> ours.compareTo(theirs)
                else -> byLength
            }
        }

        private const val DOT = '.'
        private const val ZERO = '0'
        private const val NINE = '9'
        private const val LAST_ASCII_PUNCTUATION = '~'
    }
}
