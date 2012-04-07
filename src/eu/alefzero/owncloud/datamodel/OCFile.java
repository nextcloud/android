/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
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

package eu.alefzero.owncloud.datamodel;

import java.util.Vector;

import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import android.accounts.Account;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class OCFile {
  private static String TAG = "OCFile";
  
  private long id_;
  private long length_;
  private long creation_timestamp_;
  private long modified_timestamp_;
  private String path_;
  private String storage_path_;
  private String mimetype_;
  
  private ContentProvider cp_;
  private Account account_;
  
  public OCFile(ContentProvider cp, Account account, long id) {
    cp_ = cp;
    account_ = account;
    Cursor c = cp_.query(ProviderTableMeta.CONTENT_URI_FILE,
        null,
        ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
        ProviderTableMeta._ID + "=?",
        new String[]{account_.name, String.valueOf(id)},
        null);
    setFileData(c);
  }
  
  public OCFile(ContentProvider cp, Account account, String path) {
    cp_ = cp;
    account_ = account;
    Cursor c = cp_.query(ProviderTableMeta.CONTENT_URI_FILE,
        null,
        ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
        ProviderTableMeta.FILE_PATH + "=?",
        new String[]{account_.name, path},
        null);
    setFileData(c);
    if (path_ != null) path_ = path;
  }
  
  public long getFileId() { return id_; }
  
  public String getPath() { return path_; }
  
  public boolean fileExtist() { return id_ != -1; }
  
  public boolean isDirectory() { return mimetype_ != null && mimetype_.equals("dir"); }
  
  public boolean isDownloaded() { return storage_path_ != null; }
  
  public String getStoragePath() { return storage_path_; }
  public void setStoragePath(String storage_path) { storage_path_ = storage_path; }
  
  public long getCreationTimestamp() { return creation_timestamp_; }
  public void setCreationTimestamp(long creation_timestamp) { creation_timestamp_ = creation_timestamp; }

  public long getModificationTimestamp() { return modified_timestamp_; }
  public void setModificationTimestamp(long modification_timestamp) { modified_timestamp_ = modification_timestamp; }

  public void save() {
    ContentValues cv = new ContentValues();
    cv.put(ProviderTableMeta.FILE_MODIFIED, modified_timestamp_);
    cv.put(ProviderTableMeta.FILE_CREATION, creation_timestamp_);
    cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, length_);
    cv.put(ProviderTableMeta.CONTENT_TYPE, mimetype_);
    
    Uri new_entry = cp_.insert(ProviderTableMeta.CONTENT_URI, cv);
    try {
      id_ = Integer.parseInt(new_entry.getEncodedPath());
    } catch (NumberFormatException e) {
      Log.e(TAG, "Can't retrieve file id from uri: " + new_entry.toString() +
                ", reason: " + e.getMessage());
      id_ = -1; 
    }
  }
  
  public Vector<OCFile> getDirectoryContent() {
    if (isDirectory() && id_ != -1) {
      Vector<OCFile> ret = new Vector<OCFile>();

      Uri req_uri = Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR, String.valueOf(id_));
      Cursor c = cp_.query(req_uri, null, null, null, null);

      if (c.moveToFirst())
        do {
          long id = c.getLong(c.getColumnIndex(ProviderTableMeta._ID));
          OCFile child = new OCFile(cp_, account_, id);
          ret.add(child);
        } while (c.moveToNext());

      return ret;
    }
    return null;
  }
  
  private void setFileData(Cursor c) {
    id_ = -1;
    path_ = null;
    storage_path_ = null;
    mimetype_ = null;
    length_ = 0;
    creation_timestamp_ = 0;
    modified_timestamp_ = 0;  
    if (c != null && c.moveToFirst()) {
      id_ = c.getLong(c.getColumnIndex(ProviderTableMeta._ID));
      path_ = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PATH));
      storage_path_ = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH));
      mimetype_ = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE));
      length_ = c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_CONTENT_LENGTH));
      creation_timestamp_ = c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_CREATION)); 
      modified_timestamp_ = c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_MODIFIED));
    }
  }
}
