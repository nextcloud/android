/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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

package com.owncloud.android.datamodel;

import com.owncloud.android.db.ProviderMeta;

import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class FileDataStorageManagerContentProviderClientIT extends FileDataStorageManagerIT {

    @Override
    public void before() {
        sut = new FileDataStorageManager(user,
                                         targetContext
                                             .getContentResolver()
                                             .acquireContentProviderClient(ProviderMeta.ProviderTableMeta.CONTENT_URI)
        );

        super.before();
    }
}
