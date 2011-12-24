/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
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

package eu.alefzero.owncloud;

import java.io.File;

import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class FileListActionListAdapter implements ListAdapter {

  private Context mContext;
  private Account mAccount;
  private String mFilename, mFileType, mFilePath, mFileStoragePath, mItemId;
  
  private final int ITEM_DOWNLOAD = 0;
  private final int ITEM_SHARE = 1;
  
  public FileListActionListAdapter(Cursor c, Context co, Account account) {
    mContext = co;
    mFilename = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_NAME));
    mFileType = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE));
    mFilePath = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PATH));
    mFileStoragePath = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH));
    mItemId = c.getString(c.getColumnIndex(ProviderTableMeta._ID));
    mAccount = account;
  }
  
  public boolean areAllItemsEnabled() {
    // TODO Auto-generated method stub
    return true;
  }

  public boolean isEnabled(int position) {
    // TODO Auto-generated method stub
    return true;
  }

  public int getCount() {
    // TODO Auto-generated method stub
    return 1;
  }

  public Object getItem(int position) {
    if (position == 0) {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      if (TextUtils.isEmpty(mFileStoragePath)) {
        intent.putExtra("toDownload", true);
        AccountManager accm = (AccountManager) mContext.getSystemService(Context.ACCOUNT_SERVICE);
        String ocurl = accm.getUserData(mAccount, AccountAuthenticator.KEY_OC_URL);
        ocurl += mFilePath + mFilename;
        intent.setData(Uri.parse(ocurl));
      } else {
        intent.putExtra("toDownload", false);
        intent.setDataAndType(Uri.fromFile(new File(mFileStoragePath)), mFileType);
      }
      return intent;
    }
    return null;
  }

  public long getItemId(int position) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getItemViewType(int position) {
    // TODO Auto-generated method stub
    return 0;
  }

  public View getView(int position, View convertView, ViewGroup parent) {
    View v = convertView;
    if (v == null) {
      LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = vi.inflate(R.layout.file_display_action_list_element, null);
    }

    TextView tv;
    ImageView iv;
    switch (position) {
      case ITEM_DOWNLOAD :
        tv = (TextView) v.findViewById(R.id.textView1);
        if (mFileStoragePath == null) {
          tv.setText("Download");
        } else {
          setActionName(tv);
        }
        iv = (ImageView) v.findViewById(R.id.imageView1);
        iv.setImageResource(R.drawable.download);
        break;
    }
    
    return v;
  }

  public int getViewTypeCount() {
    // TODO Auto-generated method stub
    return 2;
  }

  public boolean hasStableIds() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isEmpty() {
    // TODO Auto-generated method stub
    return false;
  }

  public void registerDataSetObserver(DataSetObserver observer) {
    // TODO Auto-generated method stub
    
  }

  public void unregisterDataSetObserver(DataSetObserver observer) {
    // TODO Auto-generated method stub
    
  }

  private void setActionName(TextView tv) {
    if (mFileType.matches("image/.*")) {
      tv.setText("View");
    } else if (mFileType.matches("audio/.*") || mFileType.matches("video/.*")) {
      tv.setText("Play");
    } else {
      tv.setText("Open");
    }
  }
  
}
