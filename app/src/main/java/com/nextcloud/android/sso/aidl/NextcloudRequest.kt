/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
/*
 *  Nextcloud SingleSignOn
 *
 *  @author David Luhmer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.android.sso.aidl

import com.nextcloud.android.sso.QueryParam
import java.io.Serializable
import java.util.LinkedList

class NextcloudRequest private constructor() : Serializable {
    var method: String? = null
        private set
    var header: Map<String, List<String>> = HashMap()
        private set
    var parameter: Map<String, String> = HashMap()
        private set
    @JvmField
    val parameterV2: Collection<QueryParam> = LinkedList()
    var requestBody: String? = null
        private set
    var url: String? = null
        private set
    private var token: String? = null
    var packageName: String? = null
    private var accountName: String? = null
    var isFollowRedirects = false
        private set

    class Builder {
        private val ncr: NextcloudRequest

        init {
            ncr = NextcloudRequest()
        }

        fun build(): NextcloudRequest {
            return ncr
        }

        fun setMethod(method: String?): Builder {
            ncr.method = method
            return this
        }

        fun setHeader(header: Map<String, List<String>>): Builder {
            ncr.header = header
            return this
        }

        fun setParameter(parameter: Map<String, String>): Builder {
            ncr.parameter = parameter
            return this
        }

        fun setRequestBody(requestBody: String?): Builder {
            ncr.requestBody = requestBody
            return this
        }

        fun setUrl(url: String?): Builder {
            ncr.url = url
            return this
        }

        fun setToken(token: String?): Builder {
            ncr.token = token
            return this
        }

        fun setAccountName(accountName: String?): Builder {
            ncr.accountName = accountName
            return this
        }

        /**
         * Default value: true
         * @param followRedirects
         * @return
         */
        fun setFollowRedirects(followRedirects: Boolean): Builder {
            ncr.isFollowRedirects = followRedirects
            return this
        }
    }

    fun getToken(): String? {
        return token
    }

    fun setToken(token: String?) {
        this.token = token
    }

    fun getAccountName(): String? {
        return accountName
    }

    fun setAccountName(accountName: String?) {
        this.accountName = accountName
    }

    companion object {
        private const val serialVersionUID = 215521212534240L //assign a long value
    }
}