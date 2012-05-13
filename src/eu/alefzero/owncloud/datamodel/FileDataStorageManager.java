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
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

public class FileDataStorageManager implements DataStorageManager {

  private ContentResolver mContentResolver;
  private ContentProviderClient mContentProvider;
  private Account mAccount;
  
  private static String TAG = "FileDataStorageManager";
  
  public FileDataStorageManager(Account account, ContentResolver cr) {
    mContentProvider = null;
    mContentResolver = cr;
    mAccount = account;
  }
  
  public FileDataStorageManager(Account account, ContentProviderClient cp) {
    mContentProvider = cp;
    mContentResolver = null;
    mAccount = account;
  }
  
  @Override
  public OCFile getFileByPath(String path) {
    Cursor c = getCursorForValue(ProviderTableMeta.FILE_PATH, path);
    OCFile file = null;
    if (c.moveToFirst()) {
      file = createFileInstance(c);
      c.close();
    }
    return file;
  }

  @Override
  public OCFile getFileById(long id) {
    Cursor c = getCursorForValue(ProviderTableMeta._ID, String.valueOf(id));
    OCFile file = null;
    if (c.moveToFirst()) {
      file = createFileInstance(c);
      c.close();
    }
    return file;
  }

  @Override
  public boolean fileExists(long id) {
    return fileExists(ProviderTableMeta._ID, String.valueOf(id));
  }

  @Override
  public boolean fileExists(String path) {
    return fileExists(ProviderTableMeta.FILE_PATH, path);
  }

  @Override
  public boolean saveFile(OCFile file) {
    boolean overriden = false;
    ContentValues cv = new ContentValues();
    cv.put(ProviderTableMeta.FILE_MODIFIED, file.getModificationTimestamp());
    cv.put(ProviderTableMeta.FILE_CREATION, file.getCreationTimestamp());
    cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, file.getFileLength());
    cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, file.getMimetype());
    cv.put(ProviderTableMeta.FILE_NAME, file.getFileName());
    if (file.getParentId() != 0)
      cv.put(ProviderTableMeta.FILE_PARENT, file.getParentId());
    cv.put(ProviderTableMeta.FILE_PATH, file.getPath());
    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());
    cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, mAccount.name);

    if (fileExists(file.getPath())) {
      OCFile tmpfile = getFileByPath(file.getPath());
      file.setStoragePath(tmpfile.getStoragePath());
      cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());
      file.setFileId(tmpfile.getFileId());
      
      overriden = true;
      if (getContentResolver() != null) {
        getContentResolver().update(ProviderTableMeta.CONTENT_URI,
                                    cv,
                                    ProviderTableMeta._ID + "=?",
                                    new String[] {String.valueOf(file.getFileId())});
      } else {
        try {
          getContentProvider().update(ProviderTableMeta.CONTENT_URI,
                                      cv,
                                      ProviderTableMeta._ID + "=?",
                                      new String[] {String.valueOf(file.getFileId())});
        } catch (RemoteException e) {
          Log.e(TAG, "Fail to insert insert file to database " + e.getMessage());
        }
      }
    } else {
      Uri result_uri = null;
      if (getContentResolver() != null) {
        result_uri = getContentResolver().insert(ProviderTableMeta.CONTENT_URI_FILE, cv);
      } else {
        try {
          result_uri = getContentProvider().insert(ProviderTableMeta.CONTENT_URI_FILE, cv);
        } catch (RemoteException e) {
          Log.e(TAG, "Fail to insert insert file to database " + e.getMessage());
        }
      }
      if (result_uri != null) {
        long new_id = Long.parseLong(result_uri.getPathSegments().get(1));
        file.setFileId(new_id);
      }
    }

    if (file.isDirectory() && file.needsUpdatingWhileSaving())
      for (OCFile f : getDirectoryContent(file))
        saveFile(f);
    
    return overriden;
  }

  public void setAccount(Account account) {
    mAccount = account;
  }
  
  public Account getAccount() {
    return mAccount;
  }
  
  public void setContentResolver(ContentResolver cr) {
    mContentResolver = cr;
  }
  
  public ContentResolver getContentResolver() {
    return mContentResolver;
  }
  
  public void setContentProvider(ContentProviderClient cp) {
    mContentProvider = cp;
  }
  
  public ContentProviderClient getContentProvider() {
    return mContentProvider;
  }

  public Vector<OCFile> getDirectoryContent(OCFile f) {
    if (f != null && f.isDirectory() && f.getFileId() != -1) {
      Vector<OCFile> ret = new Vector<OCFile>();

      Uri req_uri = Uri.withAppendedPath(
          ProviderTableMeta.CONTENT_URI_DIR, String.valueOf(f.getFileId()));
      Cursor c = null;
      
      if (getContentProvider() != null) {
        try {
          c = getContentProvider().query(req_uri,
                                         null,
                                         ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                                         new String[]{mAccount.name},
                                         null);
        } catch (RemoteException e) {
          Log.e(TAG, e.getMessage());
          return ret;
        }
      } else {
        c = getContentResolver().query(req_uri,
                                       null,
                                       ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                                       new String[]{mAccount.name},
                                       null);
      }

      if (c.moveToFirst()) {
        do {
          OCFile child = createFileInstance(c);
          ret.add(child);
        } while (c.moveToNext());
      }
      
      c.close();
      return ret;
    }
    return null;
  }

  
  private boolean fileExists(String cmp_key, String value) {
    Cursor c;
    if (getContentResolver() != null) {
      c = getContentResolver().query(ProviderTableMeta.CONTENT_URI,
                                    null,
                                    cmp_key + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                                    new String[] {value, mAccount.name},
                                    null);
    } else {
      try {
        c = getContentProvider().query(ProviderTableMeta.CONTENT_URI,
                                      null,
                                      cmp_key + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                                      new String[] {value, mAccount.name},
                                      null);
      } catch (RemoteException e) {
        Log.e(TAG, "Couldn't determine file existance, assuming non existance: " + e.getMessage());
        return false;
      }
    }
    boolean retval = c.moveToFirst();
    c.close();
    return retval;
  }
  
  private Cursor getCursorForValue(String key, String value) {
    Cursor c = null;
    if (getContentResolver() != null) {
      c = getContentResolver().query(ProviderTableMeta.CONTENT_URI,
                                     null,
                                     key + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                                     new String[] {value, mAccount.name},
                                     null);
    } else {
      try {
        c = getContentProvider().query(ProviderTableMeta.CONTENT_URI,
                                       null,
                                       key + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                                       new String[]{value, mAccount.name},
                                       null);
      } catch (RemoteException e) {
        Log.e(TAG, "Could not get file details: " + e.getMessage());
        c = null;
      }
    }
    return c;
  }

  private OCFile createFileInstance(Cursor c) {
    OCFile file = null;
    if (c != null) {
      file = new OCFile(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PATH)));
      file.setFileId(c.getLong(c.getColumnIndex(ProviderTableMeta._ID)));
      file.setParentId(c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_PARENT)));
      file.setStoragePath(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH)));
      file.setMimetype(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE)));
      file.setFileLength(c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_CONTENT_LENGTH)));
      file.setCreationTimestamp(c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_CREATION)));
      file.setModificationTimestamp(c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_MODIFIED)));
    }
    return file;
  }

}
