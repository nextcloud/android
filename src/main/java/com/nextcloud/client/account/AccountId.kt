package com.nextcloud.client.account

import java.io.Serializable
import java.lang.IllegalArgumentException
import java.net.URI
import java.net.URISyntaxException

/**
 * This class represents user account ID.
 *
 * Account IDs are in form of <username>@<server_host> quasi-email.
 *
 * Parser uses RFC 2396 rules to validate ID correctness, which is
 * not ideal. Since account ID format is more of a convention rather
 * than strict specification, this is still good improvement over
 * free-form strings.
 */
class AccountId private constructor(
    val username: String,
    val host: String,
    private val accountId: String = "$username@$host"
) : CharSequence by accountId, Serializable {

    companion object {
        @JvmStatic
        private val serialVersionUid = 0L

        /**
         * Create account id from username and backend server URI.
         *
         * @throws IllegalArgumentException if input username and host are empty
         */
        fun create(username: CharSequence, backendUri: URI): AccountId {
            if (username.length == 0 || backendUri.host.length == 0) {
                throw IllegalArgumentException("Cannot create account ID with empty username or backend host")
            }
            return AccountId(username.toString(), backendUri.host)
        }

        /**
         * Parse account id from free-form string.
         *
         * @throws IllegalArgumentException if input string is not a valid account ID
         */
        fun parse(accountId: CharSequence): AccountId {
            val uri = try {
                URI("dummy", accountId.toString(), null, null)
            } catch (e: URISyntaxException) {
                throw IllegalArgumentException("Malformed account id", e)
            }
            if (uri.host.isNullOrEmpty() || uri.host.endsWith(".") || uri.userInfo.isNullOrEmpty()) {
                throw IllegalArgumentException("Malformed account id: [$accountId]")
            }
            return AccountId(
                username = uri.userInfo,
                host = uri.host
            )
        }
    }

    override fun toString(): String = accountId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccountId

        if (accountId != other.accountId) return false

        return true
    }

    fun idEquals(other: CharSequence): Boolean = accountId.contentEquals(other)

    override fun hashCode(): Int = accountId.hashCode()
}
