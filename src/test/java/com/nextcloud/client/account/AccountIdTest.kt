package com.nextcloud.client.account

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import java.lang.StringBuilder
import java.net.URI
import java.nio.CharBuffer

/**
 * This test does not even try to validate the "email-like" account ID.
 * We just do bare minimum here and the test makes a nice scaffolding for
 * future work too.
 *
 * RFC 2396 parsing rules seem to be doing ok here.
 *
 * https://emailregex.com/
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    AccountIdTest.Valid::class,
    AccountIdTest.Invalid::class,
    AccountIdTest.Equals::class
)
class AccountIdTest {

    class Valid {

        data class Row(val username: String, val host: String, val id: String = "$username@$host")

        private val validIds = listOf(
            Row("username", "hostname.com"),
            Row("user_name", "hostname.com"),
            Row("user_name_100", "hostname.com"),
            Row("username", "hostname"),
            Row("username", "host.domain.tld"),
            Row("name.surname", "host.domain.tld")
        )

        @Test
        fun `parse valid account id`() {
            validIds.forEach {
                val id = AccountId.parse(it.id)
                assertEquals(id.toString(), it.id)
                assertTrue(it.id.contentEquals(id))
                assertEquals(it.host, id.host)
                assertEquals(it.username, id.username)
            }
        }

        @Test
        fun `parse from username and host uri`() {
            validIds.forEach {
                val backendUri = URI("https", it.host, null, null)
                val id = AccountId.create(it.username, backendUri)
                assertEquals(CharBuffer.wrap(it.id), CharBuffer.wrap(id))
                assertEquals(it.host, id.host)
                assertEquals(it.username, id.username)
            }
        }
    }

    class Invalid {

        val invalidAccountIds = listOf(
            "", // empty
            "localhost.localdomain", // no user
            "user@", // user without domain
            "@localhost.localdomain", // empty user
            "user:pasword@hostname.com", // user with password
            "username@hostname.com.", // dot after
            "username@.hostname.com", // dot before
            "user@name@hostname..com", // double dots
            "username#hostname.com", // illegal user separator
            "user%name@host!name.com", // illegal chars in domain
            "username@host:name.com" // illegal chars in domain
        )

        @Test
        fun `parsing fails with exception`() {
            for (id in invalidAccountIds) {
                try {
                    val parsed = AccountId.parse(id)
                    fail("Expected ${IllegalArgumentException::class} exception while parsing $id. Parsed: $parsed")
                } catch (e: IllegalArgumentException) {
                    // pass
                }
            }
        }
    }

    class Equals {

        @Test
        fun `equal ids`() {
            // WHEN
            //      ids are equal
            val id1 = AccountId.parse("user@hostname")
            val id2 = AccountId.parse("user@hostname")

            // THEN
            //      ids evaluate as equal
            //      hash code are equal
            assertEquals(id1, id2)
            assertEquals(id1.hashCode(), id2.hashCode())
        }

        @Test
        fun `not equal ids`() {
            // WHEN
            //      ids are different
            val id1 = AccountId.parse("user@hostname.com")
            val id2 = AccountId.parse("user@hostname.net")

            // THEN
            //      ids are not equal
            //      hash codes are not equal
            assertNotEquals(id1, id2)
            assertNotEquals(id1.hashCode(), id2.hashCode())
        }

        @Test
        fun `id equals other char sequence`() {
            // WHEN
            //      id is compared against different type
            //      string representation is equal
            val id = AccountId.parse("user@hostname")
            val sb = StringBuilder("user@hostname")

            // THEN
            //      ids are equal
            assertTrue(id.idEquals(sb))
        }

        @Test
        fun `id not equals other char sequence`() {
            // WHEN
            //      id is compared against different type
            //      string representation is different
            val id = AccountId.parse("user@hostname.com")
            val sb = StringBuilder("user@hostname.net")

            // THEN
            //      ids are not equal
            assertFalse(id.idEquals(sb))
        }
    }
}
