/*
 * Nextcloud SingleSignOn
 *
 * @author Timo Triebensky
 * Copyright (C) 2021 Timo Triebensky
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
 *
 * More information here: https://github.com/abeluck/android-streams-ipc
 *
 * ====================================================================
 *
 * The required methods of this class are copied and customized from PostMethod.
 */
package com.nextcloud.android.sso

import org.apache.commons.httpclient.methods.ByteArrayRequestEntity
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.RequestEntity
import org.apache.commons.httpclient.util.EncodingUtil
import java.util.Vector

class PatchMethod : PostMethod {
    /**
     * The buffered request body consisting of `NameValuePair`s.
     */
    private val params: Vector<*> = Vector<Any?>()

    /**
     * No-arg constructor.
     */
    constructor() : super()

    /**
     * Constructor specifying a URI.
     *
     * @param uri either an absolute or relative URI
     */
    constructor(uri: String?) : super(uri)

    /**
     * Returns <tt>"PATCH"</tt>.
     *
     * @return <tt>"PATCH"</tt>
     * @since 2.0
     */
    override fun getName(): String {
        return "PATCH"
    }

    /**
     * Returns <tt>true</tt> if there is a request body to be sent.
     *
     * @return boolean
     * @since 2.0beta1
     */
    override fun hasRequestContent(): Boolean {
        return if (!this.params.isEmpty()) {
            true
        } else {
            super.hasRequestContent()
        }
    }

    /**
     * Clears request body.
     *
     * @since 2.0beta1
     */
    override fun clearRequestBody() {
        this.params.clear()
        super.clearRequestBody()
    }

    /**
     * Generates a request entity from the patch parameters, if present.  Calls [ ][EntityEnclosingMethod.generateRequestBody] if parameters have not been set.
     *
     * @since 3.0
     */
    override fun generateRequestEntity(): RequestEntity {
        return if (!this.params.isEmpty()) {
            // Use a ByteArrayRequestEntity instead of a StringRequestEntity.
            // This is to avoid potential encoding issues.  Form url encoded strings
            // are ASCII by definition but the content type may not be.  Treating the content
            // as bytes allows us to keep the current charset without worrying about how
            // this charset will effect the encoding of the form url encoded string.
            val content = EncodingUtil.formUrlEncode(parameters, requestCharSet)
            ByteArrayRequestEntity(
                EncodingUtil.getAsciiBytes(content),
                FORM_URL_ENCODED_CONTENT_TYPE
            )
        } else {
            super.generateRequestEntity()
        }
    }
}