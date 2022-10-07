/*
 *  Nextcloud Android Library is available under MIT license
 *
 *  @author Álvaro Brey Vilas
 *  Copyright (C) 2022 Álvaro Brey Vilas
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.nextcloud.client.utils

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import third_parties.daveKoeller.AlphanumComparator

/**
 * Adapted on 2022/02/04 from https://github.com/nextcloud/server/blob/caff1023ea72bb2ea94130e18a2a6e2ccf819e5f/tests/lib/NaturalSortTest.php
 */
@RunWith(Parameterized::class)
class NaturalSortTest {

    @Parameterized.Parameter(0)
    lateinit var title: String

    @Parameterized.Parameter(1)
    lateinit var expected: Array<String>

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        @Suppress("LongMethod")
        fun data(): Iterable<Array<Any>> = listOf(
            arrayOf(
                "Different casing",
                arrayOf("aaa", "AAA", "bbb", "BBB")
            ),
            arrayOf(
                "Numbers",
                arrayOf(
                    "15.txt", "15b.txt", "123.txt", "124.txt", "abc", "abc1", "abc2", "abc10", "abc12", "def.txt",
                    "def (1).txt", "def (2).txt", "def (10).txt", "def (12).txt", "ghi.txt", "ghi 1.txt", "ghi 2.txt",
                    "ghi 10.txt", "ghi 12.txt", "z", "za", "zz"
                )
            ),
            arrayOf(
                "Chinese characters",
                arrayOf(
                    "123.txt", "abc.txt", "一.txt", "七.txt", "三.txt", "九.txt", "二.txt", "五.txt", "八.txt", "六.txt",
                    "十.txt", "十 2.txt", "十一.txt", "啊.txt", "四.txt", "波.txt", "破.txt", "莫.txt"
                )
            ),
            arrayOf(
                "With umlauts",
                arrayOf(
                    "ah.txt", "äh.txt", "Äh.txt", "oh.txt", "öh.txt", "Öh.txt", "uh.txt", "üh.txt", "Üh.txt",
                    "Üh 2.txt"
                )
            ),
            arrayOf(
                "Leading zeroes",
                arrayOf(
                    "2012-09-15 22.50.37.jpg", "2012-Card.jpg", "1584164_460s_v1.jpg", "08082008.jpg",
                    "02122011150.jpg", "03122011151.jpg", "9999999999999999999999999999991.jpg",
                    "9999999999999999999999999999992.jpg", "T 0 abc", "T 00 abc", "T 000 abc", "T 1 abc", "T 01 abc",
                    "T 001 abc", "T 2 abc", "T 02 abc", "T 3 abc", "T 03 abc"
                )
            ),
            arrayOf(
                "Trailing digits",
                arrayOf("A", "T", "T 01", "T 2", "T 003", "Zeros", "Zeros 2")
            ),
            arrayOf(
                "Special chars",
                arrayOf(
                    "[Test] Folder", "01 - January", "11 - November", "Ôle", "Test 1", "Test 01", "Test 04", "Üüü",
                    "z.[Test], z. Test"
                )
            )
        )
    }

    @Test
    fun test() {
        val sut = AlphanumComparator<String>()
        val shuffled = expected.clone().apply { shuffle() }
        val sorted = shuffled.sortedWith(sut).toTypedArray()
        Assert.assertArrayEquals("Wrong sort", expected, sorted)
    }
}
