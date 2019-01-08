/*
 *   nextCloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016  Bartosz Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
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

package org.nextcloud.providers.cursors;

import android.annotation.TargetApi;
import android.database.MatrixCursor;
import android.os.Build;
import android.provider.DocumentsContract.Document;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.utils.MimeTypeUtil;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class FileCursor extends MatrixCursor {

    public static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE,
            Document.COLUMN_FLAGS, Document.COLUMN_LAST_MODIFIED
    };

    public FileCursor(String... projection) {
        super(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
    }

    public void addFile(OCFile file) {
        if (file == null) {
            return;
        }

        final int iconRes = MimeTypeUtil.getFileTypeIconId(file.getMimeType(), file.getFileName());
        final String mimeType = file.isFolder() ? Document.MIME_TYPE_DIR : file.getMimeType();
        final String imagePath = MimeTypeUtil.isImage(file) && file.isDown() ? file.getStoragePath() : null;
        int flags = Document.FLAG_SUPPORTS_DELETE |
            Document.FLAG_SUPPORTS_WRITE |
            (imagePath != null ? Document.FLAG_SUPPORTS_THUMBNAIL : 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            flags = Document.FLAG_SUPPORTS_COPY | Document.FLAG_SUPPORTS_MOVE | Document.FLAG_SUPPORTS_REMOVE | flags;
        }

        if (file.isFolder()) {
            flags = flags | Document.FLAG_DIR_SUPPORTS_CREATE;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            flags = Document.FLAG_SUPPORTS_RENAME | flags;
        }

        newRow().add(Document.COLUMN_DOCUMENT_ID, Long.toString(file.getFileId()))
                .add(Document.COLUMN_DISPLAY_NAME, file.getFileName())
                .add(Document.COLUMN_LAST_MODIFIED, file.getModificationTimestamp())
                .add(Document.COLUMN_SIZE, file.getFileLength())
                .add(Document.COLUMN_FLAGS, flags)
                .add(Document.COLUMN_ICON, iconRes)
                .add(Document.COLUMN_MIME_TYPE, mimeType);
    }
}
