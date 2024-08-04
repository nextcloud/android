/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisplayUtilsTest {

    @Test
    public void testConvertIdn() {
        assertEquals("", DisplayUtils.convertIdn("", true));
        assertEquals("", DisplayUtils.convertIdn("", false));
        assertEquals("http://www.nextcloud.com", DisplayUtils.convertIdn("http://www.nextcloud.com", true));
        assertEquals("http://www.xn--wlkchen-90a.com", DisplayUtils.convertIdn("http://www.wölkchen.com", true));
        assertEquals("http://www.wölkchen.com", DisplayUtils.convertIdn("http://www.xn--wlkchen-90a.com", false));
    }
}
