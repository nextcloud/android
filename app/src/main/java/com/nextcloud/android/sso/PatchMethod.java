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

import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

public class PatchMethod extends PostMethod {

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
        return super.hasRequestContent();
    }

    /**
     * Clears request body.
     *
     * @since 2.0beta1
     */
    protected void clearRequestBody() {
        super.clearRequestBody();
    }

    /**
     * Generates a request entity from the patch parameters, if present.  Calls {@link
     * EntityEnclosingMethod#generateRequestBody()} if parameters have not been set.
     *
     * @since 3.0
     */
    protected RequestEntity generateRequestEntity() {
        return super.generateRequestEntity();
    }
}
