/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.android.sso;

import java.io.Serializable;

public class QueryParam implements Serializable {
    private static final long serialVersionUID = 21523240203234211L; // must be same as in SSO project

    public String key;
    public String value;

    public QueryParam(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
