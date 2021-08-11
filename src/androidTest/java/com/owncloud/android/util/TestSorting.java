package com.owncloud.android.util;

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
        String[] sortedArray = {"2012-09-15 22.50.37.jpg", "2012-Card.jpg", "1584164_460s_v1.jpg", "08082008.jpg",
                "02122011150.jpg", "03122011151.jpg", "9999999999999999999999999999991.jpg",
                "9999999999999999999999999999992.jpg", "T 0 abc", "T 00 abc", "T 000 abc", "T 1 abc", "T 01 abc",
                "T 001 abc", "T 2 abc", "T 02 abc", "T 3 abc", "T 03 abc"};

        assertTrue(sortAndTest(Arrays.asList(sortedArray)));
    }

    @Test
    public void testTrailingDigits() {
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

    @Test
    public void testSortCloudFiles() {
        List<OCFile> array = new ArrayList<>();
        array.add(new OCFile("/Joplin/0ed1778f8f88414286c13b2cbff8664e.md"));
        array.add(new OCFile("/Joplin/0edf70ab1b722172088257b3d73d6c46.md"));
        array.add(new OCFile("/Joplin/0fb063d4d18128fdf464878d18439ca3.md"));
        array.add(new OCFile("/Joplin/8a2d6a6461eb4979bb049837a8710a3d.md"));
        array.add(new OCFile("/Joplin/8b7ff9081f5944399e1fc83c6191a09f.md"));
        array.add(new OCFile("/Joplin/8c45ac421149a21f32edab06f0955839.md"));
        array.add(new OCFile("/Joplin/8cd855a41cca4cbd8a0c20e79c4f2f9e.md"));
        array.add(new OCFile("/Joplin/8f062be696e3488ca12a9d3f3f6aa10a.md"));
        array.add(new OCFile("/Joplin/8f74a571a11b4709bb1cdf75b4a51a8e.md"));
        array.add(new OCFile("/Joplin/10e27391096748b187392f913da048f8.md"));
        array.add(new OCFile("/Joplin/41bd23f62ebb4d0b64c5f78b5ad13133.md"));
        array.add(new OCFile("/Joplin/49bfe05d968c3c0612ddceb2250b3a18.md"));
        array.add(new OCFile("/Joplin/70ba2272c85353a0216577ec76d0d343.md"));
        array.add(new OCFile("/Joplin/90f61d36f4dd4ef79525b7b0812213e2.md"));
        array.add(new OCFile("/Joplin/448d96a6c76b48b68030c8c11339088d.md"));
        array.add(new OCFile("/Joplin/730c09e169bc4289af163a34d7fe2553.md"));
        array.add(new OCFile("/Joplin/736be455ce8aff18694649001a74722a.md"));
        array.add(new OCFile("/Joplin/749c837930174e828620a99a9e7ad7bc.md"));
        array.add(new OCFile("/Joplin/4292ef3223334677bed0ad0268e5bd4b.md"));
        array.add(new OCFile("/Joplin/4969e02c5c504899b17a355e61989bed.md"));
        array.add(new OCFile("/Joplin/7172f37973434fe3aeb6ef96bb48d968.md"));
        array.add(new OCFile("/Joplin/9846fdafaffa718ebcfa19474e8be251.md"));
        array.add(new OCFile("/Joplin/41890bd1b11a44f7a6b226120ee1efbc.md"));
        array.add(new OCFile("/Joplin/73098c81e1ae4831b3a90ffda82ddcec.md"));
        array.add(new OCFile("/Joplin/701409a07ee4464fb82d9bc0c451f204.md"));
        array.add(new OCFile("/Joplin/4093463de91947b990fccff4a1d13d8f.md"));
        array.add(new OCFile("/Joplin/72068539d77c6e7b3271b653422d6e76.md"));
        array.add(new OCFile("/Joplin/4307355783f4108aad5eba4d1b81ff91.md"));
        array.add(new OCFile("/Joplin/71563833027a49dcaa86208a3f621402.md"));
        array.add(new OCFile("/Joplin/41742897324842849667274b2d1a0bb6.md"));
        array.add(new OCFile("/Joplin/a441cb730c22451aa4352129231fe898.md"));
        array.add(new OCFile("/Joplin/a551ce1ae06e6f05669a0cadc4eb2f7b.md"));
        array.add(new OCFile("/Joplin/a590cd1671b648c7a3bae0d6fbb7da81.md"));

        assertTrue(sortAndTest(array));
    }

    @Test
    public void testSortCloudFilesWithDots() {
        List<OCFile> array = new ArrayList<>();

        array.add(new OCFile("/1.0.1"));
        array.add(new OCFile("/11.04.8933"));
        array.add(new OCFile("/121.04.8933"));
        array.add(new OCFile("/1234.04.8933"));
        array.add(new OCFile("/2010.01.66"));
        array.add(new OCFile("/2010.01.84"));
        array.add(new OCFile("/2010.02.2724"));
        array.add(new OCFile("/2010.02.6786"));
        array.add(new OCFile("/2010.03.5635"));
        array.add(new OCFile("/2010.04.5241"));
        array.add(new OCFile("/2010.04.9515"));
        array.add(new OCFile("/2010.04.9549"));
        array.add(new OCFile("/2010.05.1230"));
        array.add(new OCFile("/2010.05.7857"));
        array.add(new OCFile("/2010.06.1591"));
        array.add(new OCFile("/2010.06.5763"));
        array.add(new OCFile("/2010.06.7521"));
        array.add(new OCFile("/2010.07.4053"));
        array.add(new OCFile("/2010.07.81260"));
        array.add(new OCFile("/2010.08.5827"));
        array.add(new OCFile("/2010.09.2319"));
        array.add(new OCFile("/2010.09.3989"));
        array.add(new OCFile("/2010.09.6730"));
        array.add(new OCFile("/2010.11.7728"));
        array.add(new OCFile("/2010.11.81271"));
        array.add(new OCFile("/2011.01.3175"));
        array.add(new OCFile("/2011.02.1659"));
        array.add(new OCFile("/2011.02.3068"));
        array.add(new OCFile("/2011.02.9824"));
        array.add(new OCFile("/2011.03.7712"));
        array.add(new OCFile("/2011.04.4308"));
        array.add(new OCFile("/2011.04.5001"));
        array.add(new OCFile("/2011.05.5452"));
        array.add(new OCFile("/2011.05.5902"));
        array.add(new OCFile("/2011.06.1760"));
        array.add(new OCFile("/2011.06.1937"));
        array.add(new OCFile("/2011.06.2733"));
        array.add(new OCFile("/2011.06.2954"));
        array.add(new OCFile("/2011.06.2990"));
        array.add(new OCFile("/2011.07.1017"));
        array.add(new OCFile("/2011.07.6014"));
        array.add(new OCFile("/2011.08.2065"));
        array.add(new OCFile("/2011.08.2122"));
        array.add(new OCFile("/2011.08.2295"));
        array.add(new OCFile("/2011.08.7430"));
        array.add(new OCFile("/2011.09.1398"));
        array.add(new OCFile("/2011.09.2769"));
        array.add(new OCFile("/2011.09.7857"));
        array.add(new OCFile("/2011.10.1147"));
        array.add(new OCFile("/2011.10.2285"));
        array.add(new OCFile("/2011.10.4875"));
        array.add(new OCFile("/2011.10.7269"));
        array.add(new OCFile("/2011.11.3716"));
        array.add(new OCFile("/2011.11.4597"));
        array.add(new OCFile("/2011.11.5201"));
        array.add(new OCFile("/2011.11.5326"));
        array.add(new OCFile("/2011.11.5763"));
        array.add(new OCFile("/2012.01.2319"));
        array.add(new OCFile("/2012.01.2966"));
        array.add(new OCFile("/2012.01.3098"));
        array.add(new OCFile("/2012.01.9713"));
        array.add(new OCFile("/2012.02.935"));
        array.add(new OCFile("/2012.02.9249"));
        array.add(new OCFile("/2012.03.2416"));
        array.add(new OCFile("/2012.03.2638"));
        array.add(new OCFile("/2012.03.3599"));
        array.add(new OCFile("/2012.03.3784"));
        array.add(new OCFile("/2012.03.8055"));
        array.add(new OCFile("/2012.05.9095"));
        array.add(new OCFile("/2012.06.206"));
        array.add(new OCFile("/2012.06.7947"));
        array.add(new OCFile("/2012.07.2853"));
        array.add(new OCFile("/2012.08.170"));
        array.add(new OCFile("/2012.08.841"));
        array.add(new OCFile("/2012.08.3684"));
        array.add(new OCFile("/2012.08.8905"));
        array.add(new OCFile("/2012.09.5068"));
        array.add(new OCFile("/2012.11.2195"));
        array.add(new OCFile("/2012.11.6968"));
        array.add(new OCFile("/2012.11.9046"));
        array.add(new OCFile("/2013.02.861"));
        array.add(new OCFile("/2013.04.1710"));
        array.add(new OCFile("/2013.04.2152"));
        array.add(new OCFile("/2013.04.3372"));
        array.add(new OCFile("/2013.05.1754"));
        array.add(new OCFile("/2013.05.2216"));
        array.add(new OCFile("/2013.05.7761"));
        array.add(new OCFile("/2013.05.8741"));
        array.add(new OCFile("/2013.07.1882"));
        array.add(new OCFile("/2013.07.6003"));
        array.add(new OCFile("/2013.08.8660"));
        array.add(new OCFile("/2013.09.7070"));
        array.add(new OCFile("/2013.09.7388"));
        array.add(new OCFile("/2013.10.804"));
        array.add(new OCFile("/2013.10.1946"));
        array.add(new OCFile("/2013.11.1929"));
        array.add(new OCFile("/2013.11.2766"));
        array.add(new OCFile("/2013.11.8259"));
        array.add(new OCFile("/2014.02.3514"));
        array.add(new OCFile("/2014.02.4056"));
        array.add(new OCFile("/2014.02.5390"));
        array.add(new OCFile("/2014.02.6094"));
        array.add(new OCFile("/2014.02.8980"));
        array.add(new OCFile("/2014.03.8426"));
        array.add(new OCFile("/2014.04.105"));
        array.add(new OCFile("/2014.04.3249"));
        array.add(new OCFile("/2014.04.8584"));
        array.add(new OCFile("/2014.05.2610"));
        array.add(new OCFile("/2014.05.5375"));
        array.add(new OCFile("/2014.05.7808"));
        array.add(new OCFile("/2014.06.2181"));
        array.add(new OCFile("/2014.07.2094"));
        array.add(new OCFile("/2014.07.7095"));
        array.add(new OCFile("/2014.07.7342"));
        array.add(new OCFile("/2014.08.365"));
        array.add(new OCFile("/2014.08.7792"));
        array.add(new OCFile("/2014.08.7971"));
        array.add(new OCFile("/2014.09.4869"));
        array.add(new OCFile("/2014.09.8531"));
        array.add(new OCFile("/2014.09.9809"));
        array.add(new OCFile("/2014.11.3443"));
        array.add(new OCFile("/2014.11.8557"));
        array.add(new OCFile("/2015.01.812"));
        array.add(new OCFile("/2015.01.8283"));
        array.add(new OCFile("/2015.02.1406"));
        array.add(new OCFile("/2015.02.2764"));
        array.add(new OCFile("/2015.02.6614"));
        array.add(new OCFile("/2015.03.3019"));
        array.add(new OCFile("/2015.04.2022"));
        array.add(new OCFile("/2015.04.5635"));
        array.add(new OCFile("/2015.04.7370"));
        array.add(new OCFile("/2015.04.9059"));
        array.add(new OCFile("/2015.05.3916"));
        array.add(new OCFile("/2015.05.4675"));
        array.add(new OCFile("/2015.05.5681"));
        array.add(new OCFile("/2015.05.7043"));
        array.add(new OCFile("/2015.05.7767"));
        array.add(new OCFile("/2015.05.9830"));
        array.add(new OCFile("/2015.06.7024"));
        array.add(new OCFile("/2015.06.7667"));
        array.add(new OCFile("/2015.07.845"));
        array.add(new OCFile("/2015.07.8494"));
        array.add(new OCFile("/2015.08.2715"));
        array.add(new OCFile("/2015.08.3915"));
        array.add(new OCFile("/2015.09.7108"));
        array.add(new OCFile("/2015.09.9047"));
        array.add(new OCFile("/2015.10.645"));
        array.add(new OCFile("/2015.11.1950"));
        array.add(new OCFile("/2015.11.3243"));
        array.add(new OCFile("/2015.11.9182"));
        array.add(new OCFile("/2016.01.9777"));
        array.add(new OCFile("/2016.02.5648"));
        array.add(new OCFile("/2016.03.8389"));
        array.add(new OCFile("/2016.04.6043"));
        array.add(new OCFile("/2016.04.6305"));
        array.add(new OCFile("/2016.05.4529"));
        array.add(new OCFile("/2016.05.6597"));
        array.add(new OCFile("/2016.05.8586"));
        array.add(new OCFile("/2016.06.782"));
        array.add(new OCFile("/2016.06.1030"));
        array.add(new OCFile("/2016.06.5603"));
        array.add(new OCFile("/2016.07.5437"));
        array.add(new OCFile("/2016.08.2250"));
        array.add(new OCFile("/2016.08.7554"));
        array.add(new OCFile("/2016.08.7836"));
        array.add(new OCFile("/2016.08.8918"));
        array.add(new OCFile("/2016.09.3652"));
        array.add(new OCFile("/2016.09.4720"));
        array.add(new OCFile("/2016.09.6503"));
        array.add(new OCFile("/2016.10.38"));
        array.add(new OCFile("/2016.10.1857"));
        array.add(new OCFile("/2016.10.8252"));
        array.add(new OCFile("/2016.11.4425"));
        array.add(new OCFile("/2017.01.962"));
        array.add(new OCFile("/2017.01.1958"));
        array.add(new OCFile("/2017.01.2733"));
        array.add(new OCFile("/2017.01.4330"));
        array.add(new OCFile("/2017.01.7891"));
        array.add(new OCFile("/2017.02.41"));
        array.add(new OCFile("/2017.02.593"));
        array.add(new OCFile("/2017.02.1957"));
        array.add(new OCFile("/2017.02.5664"));
        array.add(new OCFile("/2017.03.786"));
        array.add(new OCFile("/2017.03.2849"));
        array.add(new OCFile("/2017.03.4025"));
        array.add(new OCFile("/2017.03.4545"));
        array.add(new OCFile("/2017.04.4579"));
        array.add(new OCFile("/2017.04.5291"));
        array.add(new OCFile("/2017.04.7168"));
        array.add(new OCFile("/2017.05.1116"));
        array.add(new OCFile("/2017.06.1672"));
        array.add(new OCFile("/2017.07.2056"));
        array.add(new OCFile("/2017.07.3936"));
        array.add(new OCFile("/2017.07.6682"));
        array.add(new OCFile("/2017.07.7894"));
        array.add(new OCFile("/2017.08.1603"));
        array.add(new OCFile("/2017.08.7493"));
        array.add(new OCFile("/2017.08.7545"));
        array.add(new OCFile("/2017.09.1846"));
        array.add(new OCFile("/2017.09.3665"));
        array.add(new OCFile("/2017.09.3978"));
        array.add(new OCFile("/2017.11.920"));
        array.add(new OCFile("/2017.11.5763"));
        array.add(new OCFile("/112233.04.8933"));
        array.add(new OCFile("/1231232.4.8933"));
        array.add(new OCFile("/1231232.40.8933"));
        array.add(new OCFile("/1231232.040.8933"));

        assertTrue(sortAndTest(array));
    }

    @Test
    public void testSortCloudFilesWithDotFilesAndFolders() {
        List<OCFile> sortedArray = new ArrayList<>();

        sortedArray.add(new OCFile("/.apache2").setFolder());
        sortedArray.add(new OCFile("/.cache").setFolder());
        sortedArray.add(new OCFile("/.config").setFolder());
        sortedArray.add(new OCFile("/.local").setFolder());
        sortedArray.add(new OCFile("/.logs").setFolder());
        sortedArray.add(new OCFile("/.nano").setFolder());
        sortedArray.add(new OCFile("/.nginx").setFolder());
        sortedArray.add(new OCFile("/.script-credentials").setFolder());
        sortedArray.add(new OCFile("/.ssh").setFolder());
        sortedArray.add(new OCFile("/.subversion").setFolder());
        sortedArray.add(new OCFile("/.znc").setFolder());
        sortedArray.add(new OCFile("/.bash_aliases"));
        sortedArray.add(new OCFile("/.bash_history"));
        sortedArray.add(new OCFile("/.bash_logout"));
        sortedArray.add(new OCFile("/.bashrc"));
        sortedArray.add(new OCFile("/.feral_aliases"));
        sortedArray.add(new OCFile("/.mysql_history"));
        sortedArray.add(new OCFile("/.profile"));
        sortedArray.add(new OCFile("/.selected_editor"));
        sortedArray.add(new OCFile("/.wget-hsts"));

        assertTrue(sortAndTest(sortedArray));
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

                System.out.println("target:");

                for (Object o : target) {
                    if (o instanceof OCFile) {
                        System.out.println(((OCFile) o).getFileName());
                    } else {
                        System.out.println(o.toString());
                    }
                }

                System.out.println("actual:");
                for (Object o : actual) {
                    if (o instanceof OCFile) {
                        System.out.println(((OCFile) o).getFileName());
                    } else {
                        System.out.println(o.toString());
                    }
                }


                return false;
            }
        }

        return true;
    }
}
