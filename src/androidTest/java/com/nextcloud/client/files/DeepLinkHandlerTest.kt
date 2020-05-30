/**
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
package com.nextcloud.client.files

import android.net.Uri
import com.nextcloud.client.account.Server
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Suite
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.net.URI

@RunWith(Suite::class)
@Suite.SuiteClasses(
    DeepLinkHandlerTest.DeepLinkPattern::class,
    DeepLinkHandlerTest.FileDeepLink::class
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
                    INDEX_PHP_PATH.forEach { indexPath ->
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

        @Test
        fun no_trailing_path_allowed_after_file_id() {
            val invalidUrl = "$url/"
            val match = DeepLinkHandler.DEEP_LINK_PATTERN.matchEntire(invalidUrl)
            assertNull(match)
        }
    }

    class FileDeepLink {

        companion object {
            const val OTHER_SERVER_BASE_URL = "https://someotherserver.net"
            const val SERVER_BASE_URL = "https://server.net"
            const val FILE_ID = "1234567890"
            val DEEP_LINK = Uri.parse("$SERVER_BASE_URL/index.php/f/$FILE_ID")

            fun createMockUser(serverBaseUrl: String): User {
                val user = mock<User>()
                val uri = URI.create(serverBaseUrl)
                val server = Server(uri = uri, version = OwnCloudVersion.nextcloud_19)
                whenever(user.server).thenReturn(server)
                return user
            }
        }

        @Mock
        lateinit var userAccountManager: UserAccountManager
        lateinit var allUsers: List<User>
        lateinit var handler: DeepLinkHandler

        @Before
        fun setUp() {
            MockitoAnnotations.initMocks(this)
            whenever(userAccountManager.allUsers).thenAnswer { allUsers }
            allUsers = emptyList()
            handler = DeepLinkHandler(userAccountManager)
        }

        @Test
        fun no_user_can_open_file() {
            // GIVEN
            //      no user capable of opening the file
            allUsers = listOf(
                createMockUser(OTHER_SERVER_BASE_URL),
                createMockUser(OTHER_SERVER_BASE_URL)
            )

            // WHEN
            //      deep link is parsed
            val match = handler.parseDeepLink(DEEP_LINK)

            // THEN
            //      link is valid
            //      no user can open the file
            assertNotNull(match)
            assertEquals(0, match?.users?.size)
        }

        @Test
        fun single_user_can_open_file() {
            // GIVEN
            //      multiple users registered
            //      one user capable of opening the link
            val matchingUser = createMockUser(SERVER_BASE_URL)
            allUsers = listOf(
                createMockUser(OTHER_SERVER_BASE_URL),
                matchingUser,
                createMockUser(OTHER_SERVER_BASE_URL)
            )

            // WHEN
            //      deep link is parsed
            val match = handler.parseDeepLink(DEEP_LINK)

            // THEN
            //      link can be opened by single user
            assertNotNull(match)
            assertSame(matchingUser, match?.users?.get(0))
        }

        @Test
        fun multiple_users_can_open_file() {
            // GIVEN
            //      mutltiple users registered
            //      multiple users capable of opening the link
            val matchingUsers = setOf(
                createMockUser(SERVER_BASE_URL),
                createMockUser(SERVER_BASE_URL)
            )
            val otherUsers = setOf(
                createMockUser(OTHER_SERVER_BASE_URL),
                createMockUser(OTHER_SERVER_BASE_URL)
            )
            allUsers = listOf(matchingUsers, otherUsers).flatten()

            // WHEN
            //      deep link is parsed
            val match = handler.parseDeepLink(DEEP_LINK)

            // THEN
            //      link can be opened by multiple matching users
            assertNotNull(match)
            assertEquals(matchingUsers, match?.users?.toSet())
        }

        @Test
        fun match_contains_extracted_file_id() {
            // WHEN
            //      valid deep file link is parsed
            val match = handler.parseDeepLink(DEEP_LINK)

            // THEN
            //      file id is returned
            assertEquals(FILE_ID, match?.fileId)
        }

        @Test
        fun no_match_for_invalid_link() {
            // GIVEN
            //      invalid deep link
            val invalidLink = Uri.parse("http://www.dodgylink.com/index.php")

            // WHEN
            //      deep link is parsed
            val match = handler.parseDeepLink(invalidLink)

            // THEN
            //      no match
            assertNull(match)
        }
    }
}
