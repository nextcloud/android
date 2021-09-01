/*
 *
 *  * Nextcloud Android client application
 *  *
 *  * @author Tobias Kaminsky
 *  * Copyright (C) 2019 Tobias Kaminsky
 *  * Copyright (C) 2019 Nextcloud GmbH
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
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
