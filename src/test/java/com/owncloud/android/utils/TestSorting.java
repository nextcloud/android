package com.owncloud.android.utils;

import com.owncloud.android.datamodel.OCFile;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
        String[] sortedArray = {"[Test] Folder", "01 - January", "11 - November", "Ôle",
                "Test 1", "Test 01", "Test 04", "Üüü",
                "z.[Test], z. Test"};

        assertTrue(sortAndTest(Arrays.asList(sortedArray)));
    }

    @Test
    public void testDifferentCasing() {
        String[] sortedArray = {"aaa", "AAA", "bbb", "BBB"};

        assertTrue(sortAndTest(Arrays.asList(sortedArray)));
    }

    @Test
    public void testLeadingZeros() {
        String[] sortedArray = {"T 0 abc", "T 00 abc", "T 000 abc", "T 1 abc", "T 01 abc",
                "T 001 abc", "T 2 abc", "T 02 abc", "T 3 abc", "T 03 abc"};

        assertTrue(sortAndTest(Arrays.asList(sortedArray)));
    }

    @Test
    public void testTrailingDigits() {
        String[] unsortedArray = {"Zeros 2", "Zeros", "T 2", "T", "T 01", "T 003", "A"};
        String[] sortedArray = {"A", "T", "T 01", "T 2", "T 003", "Zeros", "Zeros 2"};

        assertTrue(sortAndTest(Arrays.asList(sortedArray)));
    }

    @Test
    public void testOCFilesWithFolderFirst() {
        List<OCFile> sortedArray = new ArrayList<>();
        sortedArray.add(new OCFile("/ah.txt").setFolder());
        sortedArray.add(new OCFile("/Äh.txt").setFolder());
        sortedArray.add(new OCFile("/oh.txt").setFolder());
        sortedArray.add(new OCFile("/öh.txt").setFolder());
        sortedArray.add(new OCFile("/üh.txt").setFolder());
        sortedArray.add(new OCFile("/Üh.txt").setFolder());
        sortedArray.add(new OCFile("/äh.txt"));
        sortedArray.add(new OCFile("/Öh.txt"));
        sortedArray.add(new OCFile("/uh.txt"));
        sortedArray.add(new OCFile("/Üh 2.txt"));

        assertTrue(sortAndTest(sortedArray));
    }

    /**
     * uses OCFile.compareTo() instead of custom comparator
     */
    @Test
    public void testOCFiles() {
        List<OCFile> sortedArray = new ArrayList<>();
        sortedArray.add(new OCFile("/ah.txt").setFolder());
        sortedArray.add(new OCFile("/Äh.txt").setFolder());
        sortedArray.add(new OCFile("/oh.txt").setFolder());
        sortedArray.add(new OCFile("/öh.txt").setFolder());
        sortedArray.add(new OCFile("/üh.txt").setFolder());
        sortedArray.add(new OCFile("/Üh.txt").setFolder());
        sortedArray.add(new OCFile("/äh.txt"));
        sortedArray.add(new OCFile("/Öh.txt"));
        sortedArray.add(new OCFile("/uh.txt"));
        sortedArray.add(new OCFile("/Üh 2.txt"));

        List unsortedList = shuffle(sortedArray);
        Collections.sort(unsortedList);

        assertTrue(test(sortedArray, unsortedList));
    }

    private List<Comparable> shuffle(List<? extends Comparable> files) {
        List<Comparable> shuffled = new ArrayList<>();
        shuffled.addAll(files);

        Collections.shuffle(shuffled);

        return shuffled;
    }

    private boolean sortAndTest(List<? extends Comparable> sortedList) {
        return test(sortedList, sort(sortedList));
    }

    private List<Comparable> sort(List<? extends Comparable> sortedList) {
        List unsortedList = shuffle(sortedList);

        if (sortedList.get(0) instanceof OCFile) {
            Collections.sort(unsortedList, (Comparator<OCFile>) (o1, o2) -> {
                if (o1.isFolder() && o2.isFolder()) {
                    return new AlphanumComparator().compare(o1, o2);
                } else if (o1.isFolder()) {
                    return -1;
                } else if (o2.isFolder()) {
                    return 1;
                }
                return new AlphanumComparator().compare(o1, o2);
            });
        } else {
            Collections.sort(unsortedList, new AlphanumComparator<>());
        }

        return unsortedList;
    }

    private boolean test(List<? extends Comparable> target, List<? extends Comparable> actual) {

        for (int i = 0; i < target.size(); i++) {
            int compare;

            if (target.get(i) instanceof OCFile) {
                String sortedName = ((OCFile) target.get(i)).getFileName();
                String unsortedName = ((OCFile) actual.get(i)).getFileName();
                compare = sortedName.compareTo(unsortedName);
            } else {
                compare = target.get(i).compareTo(actual.get(i));
            }

            if (compare != 0) {

                System.out.println(" target: \n" + target.toString());
                System.out.println(" actual: \n" + actual.toString());

                return false;
            }
        }

        return true;
    }
}
