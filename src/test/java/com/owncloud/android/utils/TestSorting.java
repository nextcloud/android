package com.owncloud.android.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import third_parties.daveKoeller.AlphanumComparator;

import static org.junit.Assert.assertTrue;

/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 Tests used from 
 https://github.com/nextcloud/server/blob/9a4253ef7c34f9dc71a6a9f7828a10df769f0c32/tests/lib/NaturalSortTest.php
 at 2017-11-21 to stay in sync with server.
 Added first test with special chars
 */

public class TestSorting {

    @Test
    public void testSpecialChars() {

        String[] unsortedArray = {"11 - November", "Test 04", "Test 01", "Ôle", "Üüü", "01 - Januar", "[Test] Folder",
                "z.[Test]", "z. Test"};

        String[] sortedArray = {"[Test] Folder", "01 - Januar", "11 - November", "Ôle", "Test 01", "Test 04", "Üüü",
                "z. Test", "z.[Test]"};

        assertTrue(sortAndTest(unsortedArray, sortedArray));
    }

    @Test
    public void testDifferentCasing() {
        String[] unsortedArray = {"aaa", "bbb", "BBB", "AAA"};
        String[] sortedArray = {"aaa", "AAA", "bbb", "BBB"};

        assertTrue(sortAndTest(unsortedArray, sortedArray));
    }

    @Test
    public void testNumbers() {
        String[] unsortedArray = {"124.txt", "abc1", "123.txt", "abc", "abc2", "def (2).txt", "ghi 10.txt", "abc12",
                "def.txt", "def (1).txt", "ghi 2.txt", "def (10).txt", "abc10", "def (12).txt", "z", "ghi.txt", "za",
                "ghi 1.txt", "ghi 12.txt", "zz", "15.txt", "15b.txt"};

        String[] sortedArray = {"15.txt", "15b.txt", "123.txt", "124.txt", "abc", "abc1", "abc2", "abc10", "abc12",
                "def.txt", "def (1).txt", "def (2).txt", "def (10).txt", "def (12).txt", "ghi.txt", "ghi 1.txt",
                "ghi 2.txt", "ghi 10.txt", "ghi 12.txt", "z", "za", "zz"};

        assertTrue(sortAndTest(unsortedArray, sortedArray));
    }

    @Test
    public void testChineseCharacters() {
        String[] unsortedArray = {"十.txt", "一.txt", "二.txt", "十 2.txt", "三.txt", "四.txt", "abc.txt", "五.txt",
                "七.txt", "八.txt", "九.txt", "六.txt", "十一.txt", "波.txt", "破.txt", "莫.txt", "啊.txt", "123.txt"};

        String[] sortedArray = {"123.txt", "abc.txt", "一.txt", "七.txt", "三.txt", "九.txt", "二.txt", "五.txt",
                "八.txt", "六.txt", "十.txt", "十 2.txt", "十一.txt", "啊.txt", "四.txt", "波.txt", "破.txt", "莫.txt"};

        assertTrue(sortAndTest(unsortedArray, sortedArray));
    }

    @Test
    public void testWithUmlauts() {
        String[] unsortedArray = {"öh.txt", "Äh.txt", "oh.txt", "Üh 2.txt", "Üh.txt", "ah.txt", "Öh.txt", "uh.txt",
                "üh.txt", "äh.txt"};
        String[] sortedArray = {"ah.txt", "äh.txt", "Äh.txt", "oh.txt", "öh.txt", "Öh.txt", "uh.txt", "üh.txt",
                "Üh.txt", "Üh 2.txt"};

        assertTrue(sortAndTest(unsortedArray, sortedArray));
    }

    private boolean sortAndTest(String[] unsortedArray, String[] sortedArray) {
        List<String> unsortedList = Arrays.asList(unsortedArray);
        List<String> sortedList = Arrays.asList(sortedArray);


        Collections.sort(unsortedList, new AlphanumComparator());

        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).compareTo(unsortedList.get(i)) != 0) {

                System.out.println(" target: " + sortedList.toString());
                System.out.println(" actual: " + unsortedList.toString());


                return false;
            }
        }

        return true;
    }
}
