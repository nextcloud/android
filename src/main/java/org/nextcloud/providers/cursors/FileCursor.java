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

import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract.Document;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.providers.DocumentsStorageProvider;
import com.owncloud.android.utils.MimeTypeUtil;

public class FileCursor extends MatrixCursor {

    static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
        Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE,
        Document.COLUMN_FLAGS, Document.COLUMN_LAST_MODIFIED
    };

    private Bundle extra;
    private AsyncTask<?, ?, ?> loadingTask;

    public FileCursor(String... projection) {
        super(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
    }

    public void setLoadingTask(AsyncTask<?, ?, ?> task) {
        this.loadingTask = task;
    }

    @Override
    public void setExtras(Bundle extras) {
        this.extra = extras;
    }

    @Override
    public Bundle getExtras() {
        return extra;
    }

    @Override
    public void close() {
        super.close();
        if (loadingTask != null && loadingTask.getStatus() != AsyncTask.Status.FINISHED) {
            loadingTask.cancel(false);
        }
    }

    public void addFile(DocumentsStorageProvider.Document document) {
        if (document == null) {
            return;
        }

        OCFile file = document.getFile();

        final int iconRes = MimeTypeUtil.getFileTypeIconId(file.getMimeType(), file.getFileName());
        final String mimeType = file.isFolder() ? Document.MIME_TYPE_DIR : file.getMimeType();
        int flags = Document.FLAG_SUPPORTS_DELETE |
            Document.FLAG_SUPPORTS_WRITE |
            (MimeTypeUtil.isImage(file) ? Document.FLAG_SUPPORTS_THUMBNAIL : 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            flags = Document.FLAG_SUPPORTS_COPY | Document.FLAG_SUPPORTS_MOVE | Document.FLAG_SUPPORTS_REMOVE | flags;
        }

        if (file.isFolder()) {
            flags = flags | Document.FLAG_DIR_SUPPORTS_CREATE;
        }

        flags = Document.FLAG_SUPPORTS_RENAME | flags;

        newRow().add(Document.COLUMN_DOCUMENT_ID, document.getDocumentId())
                .add(Document.COLUMN_DISPLAY_NAME, file.getFileName())
                .add(Document.COLUMN_LAST_MODIFIED, file.getModificationTimestamp())
                .add(Document.COLUMN_SIZE, file.getFileLength())
                .add(Document.COLUMN_FLAGS, flags)
                .add(Document.COLUMN_ICON, iconRes)
                .add(Document.COLUMN_MIME_TYPE, mimeType);
    }
}
