/* ownCloud Android client application
 *   Copyright (C) 2012-2014 ownCloud Inc.
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

package com.owncloud.android.datamodel;

import com.owncloud.android.utils.Log_OC;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;

/**
 * CursorLoader for FileList
 * 
 * @author masensio
 *
 */
public class FileListCursorLoader extends CursorLoader {

    private static final String TAG = CursorLoader.class.getSimpleName();
    
    private long mParentId;
    private FileDataStorageManager mStorageManager;
    
    public FileListCursorLoader(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public FileListCursorLoader(Context context, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        super(context, uri, projection, selection, selectionArgs, sortOrder);
        // TODO Auto-generated constructor stub
    }
    
    public FileListCursorLoader(Context context, FileDataStorageManager storageManager) {
        super(context);
        mStorageManager = storageManager;
    }
    
    public void setParentId(long parentId) {
        mParentId = parentId;
    }
    public long getParentId(){
        return mParentId;
    }
    
    public void setStorageManager(FileDataStorageManager storageManager) {
        mStorageManager = storageManager;
    }
    
    @Override
    public Cursor loadInBackground() {
        Log_OC.d(TAG, "loadInBackgroud");
        Cursor cursor = null;
        if (mStorageManager != null) {
            cursor = mStorageManager.getContent(mParentId);
        }
        return cursor;
    }
}