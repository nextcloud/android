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

package com.nextcloud.android.sso;

import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.util.EncodingUtil;

import java.util.Vector;

public class PatchMethod extends PostMethod {

    /**
     * The buffered request body consisting of <code>NameValuePair</code>s.
     */
    private Vector params = new Vector();

    /**
     * No-arg constructor.
     */
    public PatchMethod() {
        super();
    }

    /**
     * Constructor specifying a URI.
     *
     * @param uri either an absolute or relative URI
     */
    public PatchMethod(String uri) {
        super(uri);
    }

    /**
     * Returns <tt>"PATCH"</tt>.
     *
     * @return <tt>"PATCH"</tt>
     * @since 2.0
     */
    @Override
    public String getName() {
        return "PATCH";
    }

    /**
     * Returns <tt>true</tt> if there is a request body to be sent.
     *
     * @return boolean
     * @since 2.0beta1
     */
    protected boolean hasRequestContent() {
        if (!this.params.isEmpty()) {
            return true;
        } else {
            return super.hasRequestContent();
        }
    }

    /**
     * Clears request body.
     *
     * @since 2.0beta1
     */
    protected void clearRequestBody() {
        this.params.clear();
        super.clearRequestBody();
    }

    /**
     * Generates a request entity from the patch parameters, if present.  Calls {@link
     * EntityEnclosingMethod#generateRequestBody()} if parameters have not been set.
     *
     * @since 3.0
     */
    protected RequestEntity generateRequestEntity() {
        if (!this.params.isEmpty()) {
            // Use a ByteArrayRequestEntity instead of a StringRequestEntity.
            // This is to avoid potential encoding issues.  Form url encoded strings
            // are ASCII by definition but the content type may not be.  Treating the content
            // as bytes allows us to keep the current charset without worrying about how
            // this charset will effect the encoding of the form url encoded string.
            String content = EncodingUtil.formUrlEncode(getParameters(), getRequestCharSet());
            ByteArrayRequestEntity entity = new ByteArrayRequestEntity(
                EncodingUtil.getAsciiBytes(content),
                FORM_URL_ENCODED_CONTENT_TYPE
            );
            return entity;
        } else {
            return super.generateRequestEntity();
        }
    }
}
