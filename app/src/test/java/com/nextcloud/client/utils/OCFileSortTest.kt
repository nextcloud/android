/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: MIT
 */
package com.nextcloud.client.utils

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.FileSortOrder
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
/*
 Tests used from
 https://github.com/nextcloud/server/blob/9a4253ef7c34f9dc71a6a9f7828a10df769f0c32/tests/lib/NaturalSortTest.php
 at 2017-11-21 to stay in sync with server.
 Added first test with special chars
 */
@RunWith(Parameterized::class)
class OCFileSortTest {

    @Parameterized.Parameter(0)
    lateinit var title: String

    @Parameterized.Parameter(1)
    lateinit var expected: Array<OCFile>

    @Test
    fun testFileSortOrder() {
        val toSort = getShuffledList()

        FileSortOrder.SORT_A_TO_Z.sortCloudFiles(toSort)

        verifySort(toSort)
    }

    @Test
    fun testCompareTo() {
        val toSort = getShuffledList()

        toSort.sort()

        verifySort(toSort)
    }

    private fun getShuffledList(): MutableList<OCFile> {
        return expected.toMutableList().apply { shuffle() }
    }

    private fun verifySort(actual: MutableList<OCFile>) {
        val targetNames = expected.map { it.fileName }.toTypedArray()
        val actualNames = actual.map { it.fileName }.toTypedArray()

        Assert.assertArrayEquals("Wrong sort", targetNames, actualNames)
    }

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        @Suppress("LongMethod")
        fun data(): Iterable<Array<Any>> = listOf(
            arrayOf(
                "Folder first",
                arrayOf(
                    OCFile("/ah.txt").setFolder(), OCFile("/Äh.txt").setFolder(), OCFile("/oh.txt").setFolder(),
                    OCFile("/öh.txt").setFolder(), OCFile("/üh.txt").setFolder(), OCFile("/Üh.txt").setFolder(),
                    OCFile("/äh.txt"), OCFile("/Öh.txt"), OCFile("/uh.txt"), OCFile("/Üh 2.txt")
                )
            ),
            arrayOf(
                "Cloud files",
                listOf(
                    "/Joplin/0ed1778f8f88414286c13b2cbff8664e.md", "/Joplin/0edf70ab1b722172088257b3d73d6c46.md",
                    "/Joplin/0fb063d4d18128fdf464878d18439ca3.md", "/Joplin/8a2d6a6461eb4979bb049837a8710a3d.md",
                    "/Joplin/8b7ff9081f5944399e1fc83c6191a09f.md", "/Joplin/8c45ac421149a21f32edab06f0955839.md",
                    "/Joplin/8cd855a41cca4cbd8a0c20e79c4f2f9e.md", "/Joplin/8f062be696e3488ca12a9d3f3f6aa10a.md",
                    "/Joplin/8f74a571a11b4709bb1cdf75b4a51a8e.md", "/Joplin/10e27391096748b187392f913da048f8.md",
                    "/Joplin/41bd23f62ebb4d0b64c5f78b5ad13133.md", "/Joplin/49bfe05d968c3c0612ddceb2250b3a18.md",
                    "/Joplin/70ba2272c85353a0216577ec76d0d343.md", "/Joplin/90f61d36f4dd4ef79525b7b0812213e2.md",
                    "/Joplin/448d96a6c76b48b68030c8c11339088d.md", "/Joplin/730c09e169bc4289af163a34d7fe2553.md",
                    "/Joplin/736be455ce8aff18694649001a74722a.md", "/Joplin/749c837930174e828620a99a9e7ad7bc.md",
                    "/Joplin/4292ef3223334677bed0ad0268e5bd4b.md", "/Joplin/4969e02c5c504899b17a355e61989bed.md",
                    "/Joplin/7172f37973434fe3aeb6ef96bb48d968.md", "/Joplin/9846fdafaffa718ebcfa19474e8be251.md",
                    "/Joplin/41890bd1b11a44f7a6b226120ee1efbc.md", "/Joplin/73098c81e1ae4831b3a90ffda82ddcec.md",
                    "/Joplin/701409a07ee4464fb82d9bc0c451f204.md", "/Joplin/4093463de91947b990fccff4a1d13d8f.md",
                    "/Joplin/72068539d77c6e7b3271b653422d6e76.md", "/Joplin/4307355783f4108aad5eba4d1b81ff91.md",
                    "/Joplin/71563833027a49dcaa86208a3f621402.md", "/Joplin/41742897324842849667274b2d1a0bb6.md",
                    "/Joplin/a441cb730c22451aa4352129231fe898.md", "/Joplin/a551ce1ae06e6f05669a0cadc4eb2f7b.md",
                    "/Joplin/a590cd1671b648c7a3bae0d6fbb7da81.md"
                ).map { OCFile(it) }.toTypedArray()
            ),
            arrayOf(
                "With dots",
                listOf(
                    "/1.0.1",
                    "/11.04.8933",
                    "/121.04.8933",
                    "/1234.04.8933",
                    "/2010.01.66",
                    "/2010.01.84",
                    "/2010.02.2724",
                    "/2010.02.6786",
                    "/2010.03.5635",
                    "/2010.04.5241",
                    "/2010.04.9515",
                    "/2010.04.9549",
                    "/2010.05.1230",
                    "/2010.05.7857",
                    "/2010.06.1591",
                    "/2010.06.5763",
                    "/2010.06.7521",
                    "/2010.07.4053",
                    "/2010.07.81260",
                    "/2010.08.5827",
                    "/2010.09.2319",
                    "/2010.09.3989",
                    "/2010.09.6730",
                    "/2010.11.7728",
                    "/2010.11.81271",
                    "/2011.01.3175",
                    "/2011.02.1659",
                    "/2011.02.3068",
                    "/2011.02.9824",
                    "/2011.03.7712",
                    "/2011.04.4308",
                    "/2011.04.5001",
                    "/2011.05.5452",
                    "/2011.05.5902",
                    "/2011.06.1760",
                    "/2011.06.1937",
                    "/2011.06.2733",
                    "/2011.06.2954",
                    "/2011.06.2990",
                    "/2011.07.1017",
                    "/2011.07.6014",
                    "/2011.08.2065",
                    "/2011.08.2122",
                    "/2011.08.2295",
                    "/2011.08.7430",
                    "/2011.09.1398",
                    "/2011.09.2769",
                    "/2011.09.7857",
                    "/2011.10.1147",
                    "/2011.10.2285",
                    "/2011.10.4875",
                    "/2011.10.7269",
                    "/2011.11.3716",
                    "/2011.11.4597",
                    "/2011.11.5201",
                    "/2011.11.5326",
                    "/2011.11.5763",
                    "/2012.01.2319",
                    "/2012.01.2966",
                    "/2012.01.3098",
                    "/2012.01.9713",
                    "/2012.02.935",
                    "/2012.02.9249",
                    "/2012.03.2416",
                    "/2012.03.2638",
                    "/2012.03.3599",
                    "/2012.03.3784",
                    "/2012.03.8055",
                    "/2012.05.9095",
                    "/2012.06.206",
                    "/2012.06.7947",
                    "/2012.07.2853",
                    "/2012.08.170",
                    "/2012.08.841",
                    "/2012.08.3684",
                    "/2012.08.8905",
                    "/2012.09.5068",
                    "/2012.11.2195",
                    "/2012.11.6968",
                    "/2012.11.9046",
                    "/2013.02.861",
                    "/2013.04.1710",
                    "/2013.04.2152",
                    "/2013.04.3372",
                    "/2013.05.1754",
                    "/2013.05.2216",
                    "/2013.05.7761",
                    "/2013.05.8741",
                    "/2013.07.1882",
                    "/2013.07.6003",
                    "/2013.08.8660",
                    "/2013.09.7070",
                    "/2013.09.7388",
                    "/2013.10.804",
                    "/2013.10.1946",
                    "/2013.11.1929",
                    "/2013.11.2766",
                    "/2013.11.8259",
                    "/2014.02.3514",
                    "/2014.02.4056",
                    "/2014.02.5390",
                    "/2014.02.6094",
                    "/2014.02.8980",
                    "/2014.03.8426",
                    "/2014.04.105",
                    "/2014.04.3249",
                    "/2014.04.8584",
                    "/2014.05.2610",
                    "/2014.05.5375",
                    "/2014.05.7808",
                    "/2014.06.2181",
                    "/2014.07.2094",
                    "/2014.07.7095",
                    "/2014.07.7342",
                    "/2014.08.365",
                    "/2014.08.7792",
                    "/2014.08.7971",
                    "/2014.09.4869",
                    "/2014.09.8531",
                    "/2014.09.9809",
                    "/2014.11.3443",
                    "/2014.11.8557",
                    "/2015.01.812",
                    "/2015.01.8283",
                    "/2015.02.1406",
                    "/2015.02.2764",
                    "/2015.02.6614",
                    "/2015.03.3019",
                    "/2015.04.2022",
                    "/2015.04.5635",
                    "/2015.04.7370",
                    "/2015.04.9059",
                    "/2015.05.3916",
                    "/2015.05.4675",
                    "/2015.05.5681",
                    "/2015.05.7043",
                    "/2015.05.7767",
                    "/2015.05.9830",
                    "/2015.06.7024",
                    "/2015.06.7667",
                    "/2015.07.845",
                    "/2015.07.8494",
                    "/2015.08.2715",
                    "/2015.08.3915",
                    "/2015.09.7108",
                    "/2015.09.9047",
                    "/2015.10.645",
                    "/2015.11.1950",
                    "/2015.11.3243",
                    "/2015.11.9182",
                    "/2016.01.9777",
                    "/2016.02.5648",
                    "/2016.03.8389",
                    "/2016.04.6043",
                    "/2016.04.6305",
                    "/2016.05.4529",
                    "/2016.05.6597",
                    "/2016.05.8586",
                    "/2016.06.782",
                    "/2016.06.1030",
                    "/2016.06.5603",
                    "/2016.07.5437",
                    "/2016.08.2250",
                    "/2016.08.7554",
                    "/2016.08.7836",
                    "/2016.08.8918",
                    "/2016.09.3652",
                    "/2016.09.4720",
                    "/2016.09.6503",
                    "/2016.10.38",
                    "/2016.10.1857",
                    "/2016.10.8252",
                    "/2016.11.4425",
                    "/2017.01.962",
                    "/2017.01.1958",
                    "/2017.01.2733",
                    "/2017.01.4330",
                    "/2017.01.7891",
                    "/2017.02.41",
                    "/2017.02.593",
                    "/2017.02.1957",
                    "/2017.02.5664",
                    "/2017.03.786",
                    "/2017.03.2849",
                    "/2017.03.4025",
                    "/2017.03.4545",
                    "/2017.04.4579",
                    "/2017.04.5291",
                    "/2017.04.7168",
                    "/2017.05.1116",
                    "/2017.06.1672",
                    "/2017.07.2056",
                    "/2017.07.3936",
                    "/2017.07.6682",
                    "/2017.07.7894",
                    "/2017.08.1603",
                    "/2017.08.7493",
                    "/2017.08.7545",
                    "/2017.09.1846",
                    "/2017.09.3665",
                    "/2017.09.3978",
                    "/2017.11.920",
                    "/2017.11.5763",
                    "/112233.04.8933",
                    "/1231232.4.8933",
                    "/1231232.40.8933",
                    "/1231232.040.8933"
                ).map { OCFile(it) }.toTypedArray()
            ),
            arrayOf(
                "With dot files and folders",
                arrayOf(
                    OCFile("/.apache2").setFolder(),
                    OCFile("/.cache").setFolder(),
                    OCFile("/.config").setFolder(),
                    OCFile("/.local").setFolder(),
                    OCFile("/.logs").setFolder(),
                    OCFile("/.nano").setFolder(),
                    OCFile("/.nginx").setFolder(),
                    OCFile("/.script-credentials").setFolder(),
                    OCFile("/.ssh").setFolder(),
                    OCFile("/.subversion").setFolder(),
                    OCFile("/.znc").setFolder(),
                    OCFile("/.bash_aliases"),
                    OCFile("/.bash_history"),
                    OCFile("/.bash_logout"),
                    OCFile("/.bashrc"),
                    OCFile("/.feral_aliases"),
                    OCFile("/.mysql_history"),
                    OCFile("/.profile"),
                    OCFile("/.selected_editor"),
                    OCFile("/.wget-hsts")
                )
            )

        )
    }
}
