/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.db;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Meta-Class that holds various static field information
 * 
 * @author Bartek Przybylski
 * 
 */
public class ProviderMeta {

    public static final String AUTHORITY_FILES = "org.owncloud";
    public static final String DB_FILE = "owncloud.db";
    public static final String DB_NAME = "filelist";
    public static final int DB_VERSION = 4;

    private ProviderMeta() {
    }

    static public class ProviderTableMeta implements BaseColumns {
        public static final String DB_NAME = "filelist";
        public static final Uri CONTENT_URI = Uri.parse("content://"
                + AUTHORITY_FILES + "/");
        public static final Uri CONTENT_URI_FILE = Uri.parse("content://"
                + AUTHORITY_FILES + "/file");
        public static final Uri CONTENT_URI_DIR = Uri.parse("content://"
                + AUTHORITY_FILES + "/dir");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.owncloud.file";
        public static final String CONTENT_TYPE_ITEM = "vnd.android.cursor.item/vnd.owncloud.file";

        public static final String FILE_PARENT = "parent";
        public static final String FILE_NAME = "filename";
        public static final String FILE_CREATION = "created";
        public static final String FILE_MODIFIED = "modified";
        public static final String FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA = "modified_at_last_sync_for_data";
        public static final String FILE_CONTENT_LENGTH = "content_length";
        public static final String FILE_CONTENT_TYPE = "content_type";
        public static final String FILE_STORAGE_PATH = "media_path";
        public static final String FILE_PATH = "path";
        public static final String FILE_ACCOUNT_OWNER = "file_owner";
        public static final String FILE_LAST_SYNC_DATE = "last_sync_date";  // _for_properties, but let's keep it as it is
        public static final String FILE_LAST_SYNC_DATE_FOR_DATA = "last_sync_date_for_data";
        public static final String FILE_KEEP_IN_SYNC = "keep_in_sync";

        public static final String DEFAULT_SORT_ORDER = FILE_NAME
                + " collate nocase asc";

    }
}
