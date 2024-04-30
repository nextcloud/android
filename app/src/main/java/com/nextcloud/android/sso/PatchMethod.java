/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Timo Triebensky <timo@binsky.org>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 *
 * More information here: https://github.com/abeluck/android-streams-ipc
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
            return new ByteArrayRequestEntity(
                EncodingUtil.getAsciiBytes(content),
                FORM_URL_ENCODED_CONTENT_TYPE
            );
        } else {
            return super.generateRequestEntity();
        }
    }
}
