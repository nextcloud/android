/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.datamodel;

import androidx.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Model for filesystem data from the database.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileSystemDataSet {
    private int id;
    private String localPath;
    private long modifiedAt;
    private boolean folder;
    private boolean sentForUpload;
    private long foundAt;
    private long syncedFolderId;
    @Nullable private String crc32;
}
