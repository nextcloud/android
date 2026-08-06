/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.utils

import com.owncloud.android.utils.sort.AlphanumericComparator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale
import kotlin.random.Random

/**
 * Adapted from https://github.com/nextcloud/server/blob/master/tests/lib/NaturalSortTest.php
 *
 */
@Suppress("TooManyFunctions")
class NaturalSortTest {

    private lateinit var defaultLocale: Locale

    @Before
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun everyStartingOrderReachesTheExpectedOrder() {
        val comparator = AlphanumericComparator<String>()

        expectedOrders.forEach { (title, expected) ->
            repeat(SHUFFLE_COUNT) { seed ->
                val sorted = expected.shuffled(Random(seed)).sortedWith(comparator)
                assertEquals("Wrong sort for \"$title\", shuffle seed $seed", expected, sorted)
            }
        }
    }

    @Test
    fun namesDifferingOnlyByIgnorableCharacterKeepTransitivity() {
        val withSeparatorAndMark = "é-0$COMBINING_DIAERESIS"
        val withDigit = "é9"
        val withJoiner = "é$ZERO_WIDTH_JOINER"

        val firstToSecond = compare(withSeparatorAndMark, withDigit)
        val secondToThird = compare(withDigit, withJoiner)
        val firstToThird = compare(withSeparatorAndMark, withJoiner)

        assertTrue(
            "${withSeparatorAndMark.readable()} <= ${withDigit.readable()} ($firstToSecond) and " +
                "${withDigit.readable()} <= ${withJoiner.readable()} ($secondToThird) must imply " +
                "${withSeparatorAndMark.readable()} <= ${withJoiner.readable()}, but was $firstToThird",
            !(firstToSecond <= 0 && secondToThird <= 0) || firstToThird <= 0
        )
    }

    @Test
    fun comparatorIsAntisymmetric() {
        val names = namesWithIgnorableCharacters()

        val violations = names.flatMap { first ->
            names.filter { second -> compare(first, second).compareTo(0) != -compare(second, first).compareTo(0) }
                .map { second -> "${first.readable()} vs ${second.readable()}" }
        }

        assertEquals(
            "compare(a, b) must be the opposite sign of compare(b, a)",
            emptyList<String>(),
            violations.take(FAILURES_TO_REPORT)
        )
    }

    @Test
    fun comparatorIsTransitive() {
        val names = namesWithIgnorableCharacters()

        val violations = names.flatMap { first ->
            names.filter { second -> compare(first, second) <= 0 }
                .flatMap { second -> brokenChains(first, second, names) }
        }

        assertEquals("a <= b and b <= c must imply a <= c", emptyList<String>(), violations.take(FAILURES_TO_REPORT))
    }

    @Test
    fun sortingNeverThrowsRegardlessOfInputOrder() {
        val comparator = AlphanumericComparator<String>()
        val names = largeNameListWithIgnorableCharacters()

        val failures = (0 until SORT_ATTEMPTS).mapNotNull { seed ->
            runCatching { names.shuffled(Random(seed)).sortedWith(comparator) }
                .exceptionOrNull()
                ?.let { "seed $seed: ${it.message}" }
        }

        assertTrue("Sorting must never throw, but failed for ${failures.take(FAILURES_TO_REPORT)}", failures.isEmpty())
    }

    private fun brokenChains(first: String, second: String, names: List<String>): List<String> = names
        .filter { third -> compare(second, third) <= 0 && compare(first, third) > 0 }
        .map { third -> "${first.readable()} <= ${second.readable()} <= ${third.readable()}" }

    private fun compare(first: String, second: String): Int = AlphanumericComparator.compare(first, second)

    private fun String.readable(): String = buildString {
        append('"')
        this@readable.forEach { character ->
            if (character.code in PRINTABLE_RANGE) append(character) else append("\\u%04X".format(character.code))
        }
        append('"')
    }

    private fun namesWithIgnorableCharacters(): List<String> = buildNames(
        prefixes = listOf("é", "a"),
        infixes = listOf(
            "-0$COMBINING_DIAERESIS",
            "9",
            ZERO_WIDTH_JOINER,
            "",
            "-1",
            SOFT_HYPHEN,
            "8$COMBINING_DIAERESIS"
        ),
        suffixes = listOf("", ".jpg1")
    )

    private fun largeNameListWithIgnorableCharacters(): List<String> = buildNames(
        prefixes = listOf("é", "a", "ñ"),
        infixes = listOf(
            "-0$COMBINING_DIAERESIS",
            "9",
            ZERO_WIDTH_JOINER,
            "",
            "-1",
            SOFT_HYPHEN,
            "8$COMBINING_DIAERESIS",
            "-",
            "0"
        ),
        suffixes = listOf("", ".jpg1", ".jpg2")
    )

    private fun buildNames(prefixes: List<String>, infixes: List<String>, suffixes: List<String>): List<String> =
        prefixes.flatMap { prefix ->
            infixes.flatMap { infix -> suffixes.map { suffix -> prefix + infix + suffix } }
        }.distinct()

    companion object {
        private const val SHUFFLE_COUNT = 50
        private const val SORT_ATTEMPTS = 3000
        private const val FAILURES_TO_REPORT = 5
        private val PRINTABLE_RANGE = 32..126

        private const val ZERO_WIDTH_JOINER = "\u200D"
        private const val SOFT_HYPHEN = "\u00AD"
        private const val COMBINING_DIAERESIS = "\u0308"

        private val expectedOrders = listOf(
            "Different casing" to listOf("aaa", "AAA", "bbb", "BBB"),
            "Numbers" to listOf(
                "15.txt", "15b.txt", "123.txt", "124.txt", "abc", "abc1", "abc2", "abc10", "abc12", "def.txt",
                "def (1).txt", "def (2).txt", "def (10).txt", "def (12).txt", "ghi.txt", "ghi 1.txt", "ghi 2.txt",
                "ghi 10.txt", "ghi 12.txt", "z", "za", "zz"
            ),
            "Chinese characters" to listOf(
                "123.txt", "abc.txt", "一.txt", "七.txt", "三.txt", "九.txt", "二.txt", "五.txt", "八.txt", "六.txt",
                "十.txt", "十 2.txt", "十一.txt", "啊.txt", "四.txt", "波.txt", "破.txt", "莫.txt"
            ),
            "With umlauts" to listOf(
                "ah.txt", "äh.txt", "Äh.txt", "oh.txt", "öh.txt", "Öh.txt", "uh.txt", "üh.txt", "Üh.txt", "Üh 2.txt"
            ),
            "Leading zeroes" to listOf(
                "2012-09-15 22.50.37.jpg", "2012-Card.jpg", "1584164_460s_v1.jpg", "08082008.jpg",
                "02122011150.jpg", "03122011151.jpg", "9999999999999999999999999999991.jpg",
                "9999999999999999999999999999992.jpg", "T 0 abc", "T 00 abc", "T 000 abc", "T 1 abc", "T 01 abc",
                "T 001 abc", "T 2 abc", "T 02 abc", "T 3 abc", "T 03 abc"
            ),
            "Trailing digits" to listOf("A", "T", "T 01", "T 2", "T 003", "Zeros", "Zeros 2"),
            "Special chars" to listOf(
                "[Test] Folder", "01 - January", "11 - November", "Ôle", "Test 1", "Test 01", "Test 04", "Üüü",
                "z.[Test], z. Test"
            ),
            "Precomposed and decomposed accents" to listOf("Caf\u00E9.txt", "Cafe\u0301.txt"),
            "Collation ignorable characters" to listOf("file.txt", "fi${SOFT_HYPHEN}le.txt"),
            "Zero width joiner next to a number chunk" to listOf(
                "photo-1.jpg",
                "photo9.jpg",
                "photo$ZERO_WIDTH_JOINER.jpg"
            )
        )
    }
}
