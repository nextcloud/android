/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils.sort

import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import java.math.BigInteger
import java.text.Collator

/**
 * Sorts names the way people read them, so `abc2` comes before `abc10` instead of after it.
 */
class AlphanumericComparator<T : Any> : Comparator<T> {

    override fun compare(first: T, second: T): Int = compare(first.toString(), second.toString())

    companion object {

        @JvmStatic
        fun compare(first: ServerFileInterface, second: ServerFileInterface): Int =
            compare(first.fileName, second.fileName)

        @JvmStatic
        fun compare(first: String, second: String): Int {
            if (first == second) {
                return 0
            }

            val ourChunks = chunks(first)
            val theirChunks = chunks(second)

            val byChunk = ourChunks.asSequence()
                .zip(theirChunks.asSequence()) { ours, theirs -> compareChunks(ours, theirs) }
                .firstOrNull { it != 0 }

            return byChunk
                ?: ourChunks.size.compareTo(theirChunks.size).takeIf { it != 0 }
                ?: first.compareTo(second)
        }

        private val collator = ThreadLocal.withInitial { Collator.getInstance() }

        private fun chunks(name: String): List<String> {
            val chunks = mutableListOf<String>()
            var start = 0

            while (start < name.length) {
                val end = chunkEnd(name, start)
                chunks += name.substring(start, end)
                start = end
            }

            return chunks
        }

        private fun chunkEnd(name: String, start: Int): Int {
            if (name[start].isSeparator()) {
                return start + 1
            }

            val digitRun = name[start].isAsciiDigit()
            var end = start + 1
            while (end < name.length && !name[end].isSeparator() && name[end].isAsciiDigit() == digitRun) {
                end++
            }
            return end
        }

        private fun compareChunks(ours: String, theirs: String): Int {
            val ourRank = rankOf(ours)
            val byRank = ourRank.compareTo(rankOf(theirs))

            return when {
                byRank != 0 -> byRank
                ourRank == RANK_SEPARATOR -> compareSeparators(ours[0], theirs[0])
                ourRank == RANK_NUMBER -> compareNumbers(ours, theirs)
                else -> compareText(ours, theirs)
            }
        }

        private fun rankOf(chunk: String): Int = when {
            chunk[0].isSeparator() -> RANK_SEPARATOR
            chunk[0].isAsciiDigit() -> RANK_NUMBER
            else -> RANK_TEXT
        }

        private fun compareSeparators(ours: Char, theirs: Char): Int = when {
            ours == theirs -> 0
            ours == DOT -> -1
            theirs == DOT -> 1
            else -> ours.compareTo(theirs)
        }

        private fun compareNumbers(ours: String, theirs: String): Int =
            when (val byValue = BigInteger(ours).compareTo(BigInteger(theirs))) {
                0 -> leadingZeroes(ours).compareTo(leadingZeroes(theirs))
                else -> byValue
            }

        private fun leadingZeroes(digits: String): Int = digits.takeWhile { it == ZERO }.length

        private fun compareText(ours: String, theirs: String): Int {
            val byCollation = collator.get()?.compare(ours, theirs) ?: 0
            if (byCollation != 0) {
                return byCollation
            }

            return when (val byLength = ours.length.compareTo(theirs.length)) {
                0 -> ours.compareTo(theirs)
                else -> byLength
            }
        }

        private fun Char.isAsciiDigit(): Boolean = this in ZERO..NINE

        private fun Char.isSeparator(): Boolean = this <= LAST_CONTROL_OR_PUNCTUATION ||
            this in FIRST_PUNCTUATION_AFTER_DIGITS..LAST_PUNCTUATION_BEFORE_UPPERCASE ||
            this in FIRST_PUNCTUATION_AFTER_UPPERCASE..LAST_PUNCTUATION_BEFORE_LOWERCASE ||
            this in FIRST_PUNCTUATION_AFTER_LOWERCASE..LAST_ASCII_PUNCTUATION

        private const val RANK_SEPARATOR = 0
        private const val RANK_NUMBER = 1
        private const val RANK_TEXT = 2

        private const val DOT = '.'
        private const val ZERO = '0'
        private const val NINE = '9'

        private const val LAST_CONTROL_OR_PUNCTUATION = '/'
        private const val FIRST_PUNCTUATION_AFTER_DIGITS = ':'
        private const val LAST_PUNCTUATION_BEFORE_UPPERCASE = '@'
        private const val FIRST_PUNCTUATION_AFTER_UPPERCASE = '['
        private const val LAST_PUNCTUATION_BEFORE_LOWERCASE = '`'
        private const val FIRST_PUNCTUATION_AFTER_LOWERCASE = '{'
        private const val LAST_ASCII_PUNCTUATION = '~'
    }
}
