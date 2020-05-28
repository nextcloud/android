package com.nextcloud.client.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    DeepLinkHandlerTest.DeepLinkPattern::class
)
class DeepLinkHandlerTest {

    @RunWith(Parameterized::class)
    class DeepLinkPattern {

        companion object {
            val FILE_ID = 1234
            val SERVER_BASE_URLS = listOf(
                "http://hostname.net",
                "https://hostname.net",
                "http://hostname.net/subdir1",
                "https://hostname.net/subdir1",
                "http://hostname.net/subdir1/subdir2",
                "https://hostname.net/subdir1/subdir2",
                "http://hostname.net/subdir1/subdir2/subdir3",
                "https://hostname.net/subdir1/subdir2/subdir3"
            )
            val INDEX_PHP_PATH = listOf(
                "",
                "/index.php"
            )

            @Parameterized.Parameters
            @JvmStatic
            fun urls(): Array<Array<Any>> {
                val testInput = mutableListOf<Array<Any>>()
                SERVER_BASE_URLS.forEach { baseUrl ->
                    INDEX_PHP_PATH.forEach {
                        indexPath ->
                        val url = "$baseUrl$indexPath/f/$FILE_ID"
                        testInput.add(arrayOf(baseUrl, indexPath, "$FILE_ID", url))
                    }
                }
                return testInput.toTypedArray()
            }
        }

        @Parameterized.Parameter(0)
        lateinit var baseUrl: String

        @Parameterized.Parameter(1)
        lateinit var indexPath: String

        @Parameterized.Parameter(2)
        lateinit var fileId: String

        @Parameterized.Parameter(3)
        lateinit var url: String

        @Test
        fun matches_deep_link_patterns() {
            val match = DeepLinkHandler.DEEP_LINK_PATTERN.matchEntire(url)
            assertNotNull("Url [$url] does not match pattern", match)
            assertEquals(baseUrl, match?.groupValues?.get(DeepLinkHandler.BASE_URL_GROUP_INDEX))
            assertEquals(indexPath, match?.groupValues?.get(DeepLinkHandler.INDEX_PATH_GROUP_INDEX))
            assertEquals(fileId, match?.groupValues?.get(DeepLinkHandler.FILE_ID_GROUP_INDEX))
        }

    }
}
