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

import java.io.File;
import java.util.Vector;

import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

public class OCFile {
  private static String TAG = "OCFile";

  private long id_;
  private long parent_id_;
  private long length_;
  private long creation_timestamp_;
  private long modified_timestamp_;
  private String path_;
  private String storage_path_;
  private String mimetype_;

  private ContentResolver cr_;
  private ContentProviderClient cp_;
  private Account account_;

  public static OCFile createNewFile(ContentProviderClient cr, Account account,
      String path, long length, long creation_timestamp,
      long modified_timestamp, String mimetype, long parent_id) {
    OCFile new_file = new OCFile(cr, account);

    try {
      Cursor c = new_file.cp_.query(ProviderTableMeta.CONTENT_URI_FILE, null,
          ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND "
              + ProviderTableMeta.FILE_PATH + "=?", new String[]{new_file.account_.name,
              path}, null);
      if (c.moveToFirst())
        new_file.setFileData(c);
      c.close();
    } catch (RemoteException e) {
      Log.e(TAG, e.getMessage());
    }

    new_file.path_ = path;
    new_file.length_ = length;
    new_file.creation_timestamp_ = creation_timestamp;
    new_file.modified_timestamp_ = modified_timestamp;
    new_file.mimetype_ = mimetype;
    new_file.parent_id_ = parent_id;

    return new_file;
  }

  public static OCFile createNewFile(ContentResolver contentResolver, Account a,
      String path, int length, int creation_timestamp, int modified_timestamp,
      String mimetype, long parent_id) {
    OCFile new_file = new OCFile(contentResolver, a);
    Cursor c = new_file.cr_.query(ProviderTableMeta.CONTENT_URI_FILE, null,
          ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND "
              + ProviderTableMeta.FILE_PATH + "=?", new String[]{new_file.account_.name,
              path}, null);
      if (c.moveToFirst())
        new_file.setFileData(c);
      c.close();

    new_file.path_ = path;
    new_file.length_ = length;
    new_file.creation_timestamp_ = creation_timestamp;
    new_file.modified_timestamp_ = modified_timestamp;
    new_file.mimetype_ = mimetype;
    new_file.parent_id_ = parent_id;

    return new_file;
  }
  
  public OCFile(ContentResolver cr, Account account, long id) {
    cr_ = cr;
    account_ = account;
    Cursor c = cr_.query(ProviderTableMeta.CONTENT_URI_FILE, null,
        ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND "
            + ProviderTableMeta._ID + "=?",
        new String[]{account_.name, String.valueOf(id)}, null);
    if (c.moveToFirst())
      setFileData(c);
  }

  public OCFile(ContentResolver cr, Account account, String path) {
    cr_ = cr;
    account_ = account;
    
    Cursor c = cr_.query(ProviderTableMeta.CONTENT_URI_FILE, null,
        ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND "
            + ProviderTableMeta.FILE_PATH + "=?", new String[]{account_.name,
            path}, null);
    if (c.moveToFirst()) {
      setFileData(c);
      if (path_ != null)
        path_ = path;
    }
  }

  public long getFileId() {
    return id_;
  }

  public String getPath() {
    return path_;
  }

  public boolean fileExtist() {
    return id_ != -1;
  }

  public boolean isDirectory() {
    return mimetype_ != null && mimetype_.equals("DIR");
  }

  public boolean isDownloaded() {
    return storage_path_ != null;
  }

  public String getStoragePath() {
    return storage_path_;
  }
  public void setStoragePath(String storage_path) {
    storage_path_ = storage_path;
  }

  public long getCreationTimestamp() {
    return creation_timestamp_;
  }
  public void setCreationTimestamp(long creation_timestamp) {
    creation_timestamp_ = creation_timestamp;
  }

  public long getModificationTimestamp() {
    return modified_timestamp_;
  }
  public void setModificationTimestamp(long modification_timestamp) {
    modified_timestamp_ = modification_timestamp;
  }

  public String getFileName() {
    if (path_ != null) {
      File f = new File(path_);
      return f.getName().equals("") ? "/" : f.getName();
    }
    return null;
  }

  public String getMimetype() {
    return mimetype_;
  }
  
  public void save() {
    ContentValues cv = new ContentValues();
    cv.put(ProviderTableMeta.FILE_MODIFIED, modified_timestamp_);
    cv.put(ProviderTableMeta.FILE_CREATION, creation_timestamp_);
    cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, length_);
    cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, mimetype_);
    cv.put(ProviderTableMeta.FILE_NAME, getFileName());
    if (parent_id_ != 0)
      cv.put(ProviderTableMeta.FILE_PARENT, parent_id_);
    cv.put(ProviderTableMeta.FILE_PATH, path_);
    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, storage_path_);
    cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, account_.name);

    if (fileExtist()) {
      if (cp_ != null) {
        try {
          cp_.update(ProviderTableMeta.CONTENT_URI, cv, ProviderTableMeta._ID
              + "=?", new String[]{String.valueOf(id_)});
        } catch (RemoteException e) {
          Log.e(TAG, e.getMessage());
          return;
        }
      } else {
        cr_.update(ProviderTableMeta.CONTENT_URI, cv, ProviderTableMeta._ID
            + "=?", new String[]{String.valueOf(id_)});
      }
    } else {
      Uri new_entry = null;
      if (cp_ != null) { 
        try {
          new_entry = cp_.insert(ProviderTableMeta.CONTENT_URI_FILE, cv);
        } catch (RemoteException e) { 
          Log.e(TAG, e.getMessage());
          id_ = -1;
          return;
        }
      } else {
        new_entry = cr_.insert(ProviderTableMeta.CONTENT_URI_FILE, cv);
      }
      try {
        String p = new_entry.getEncodedPath();
        id_ = Integer.parseInt(p.substring(p.lastIndexOf('/')+1));
      }  catch (NumberFormatException e) {
        Log.e(TAG, "Can't retrieve file id from uri: " + new_entry.toString()
            + ", reason: " + e.getMessage());
        id_ = -1;
      }
    }
  }

  public Vector<OCFile> getDirectoryContent() {
    if (isDirectory() && id_ != -1) {
      Vector<OCFile> ret = new Vector<OCFile>();

      Uri req_uri = Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR,
          String.valueOf(id_));
      Cursor c = null;
      if (cp_ != null) {
        try {
          c = cp_.query(req_uri, null, null, null, null);
        } catch (RemoteException e) {
          Log.e(TAG, e.getMessage());
          return ret;
        }
      } else {
        c = cr_.query(req_uri, null, null, null, null);
      }

      if (c.moveToFirst())
        do {
          OCFile child = new OCFile(cp_, account_);
          child.setFileData(c);
          ret.add(child);
        } while (c.moveToNext());

      c.close();
      return ret;
    }
    return null;
  }

  public void addFile(OCFile file) {
    file.parent_id_ = id_;
    file.save();
  }

  private OCFile(ContentProviderClient cp, Account account) {
    account_ = account;
    cp_ = cp;
    resetData();
  }
  
  private OCFile(ContentResolver cr, Account account) {
    account_ = account;
    cr_ = cr;
    resetData();
  }

  private void resetData() {
    id_ = -1;
    path_ = null;
    parent_id_ = 0;
    storage_path_ = null;
    mimetype_ = null;
    length_ = 0;
    creation_timestamp_ = 0;
    modified_timestamp_ = 0;
  }

  private void setFileData(Cursor c) {
    resetData();
    if (c != null) {
      id_ = c.getLong(c.getColumnIndex(ProviderTableMeta._ID));
      path_ = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PATH));
      parent_id_ = c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_PARENT));
      storage_path_ = c.getString(c
          .getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH));
      mimetype_ = c.getString(c
          .getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE));
      length_ = c.getLong(c
          .getColumnIndex(ProviderTableMeta.FILE_CONTENT_LENGTH));
      creation_timestamp_ = c.getLong(c
          .getColumnIndex(ProviderTableMeta.FILE_CREATION));
      modified_timestamp_ = c.getLong(c
          .getColumnIndex(ProviderTableMeta.FILE_MODIFIED));
    }
  }
}
