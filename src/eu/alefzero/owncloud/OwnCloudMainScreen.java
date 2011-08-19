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

import java.util.Stack;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;

public class OwnCloudMainScreen extends ListActivity {
  private DbHandler mDBHandler;
  private Stack<String> mParents;
  private Account mAccount;
  private Cursor mCursor;
  private boolean mIsDisplayingFile;

  private static final int DIALOG_CHOOSE_ACCOUNT = 0;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mParents = new Stack<String>();
    mIsDisplayingFile = false;
    mDBHandler = new DbHandler(getBaseContext());
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.main);

    AccountManager accMan = AccountManager.get(this);
    Account[] accounts = accMan.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);

    if (accounts.length == 0) {
      // using string value since in API7 this constatn is not defined
      // in API7 < this constatant is defined in Settings.ADD_ACCOUNT_SETTINGS
      // and Settings.EXTRA_AUTHORITIES
      Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
      intent.putExtra("authorities", new String[] {AccountAuthenticator.AUTH_TOKEN_TYPE});
      startActivity(intent);
    } else if (accounts.length > 1) {
      showDialog(DIALOG_CHOOSE_ACCOUNT);
    } else {
      mAccount = accounts[0];
      populateFileList();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.settingsItem :
        Intent i = new Intent(this, Preferences.class);
        startActivity(i);
        break;
    }
    return true;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_CHOOSE_ACCOUNT:
        return createChooseAccountDialog();
      default:
        throw new IllegalArgumentException("Unknown dialog id: " + id);
    }
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);
    return true;
  }
  
  @Override
  protected void onDestroy() {
    mDBHandler.close();
    super.onDestroy();
  }
    
  private Dialog createChooseAccountDialog() {
    final AccountManager accMan = AccountManager.get(this);
    CharSequence[] items = new CharSequence[accMan.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE).length];
    int i = 0;
    for (Account a : accMan.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)) {
      items[i++] = a.name;
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.common_choose_account);
    builder.setCancelable(true);
    builder.setItems(items, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int item) {
            mAccount = accMan.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)[item];
            dialog.dismiss();
            populateFileList();
        }
    });
    builder.setOnCancelListener(new OnCancelListener() {
      public void onCancel(DialogInterface dialog) {
        OwnCloudMainScreen.this.finish();
      }
    });
    AlertDialog alert = builder.create();
    return alert;
  }

  @Override
  public void onBackPressed() {
    if (mIsDisplayingFile) {
      mIsDisplayingFile = false;
      setContentView(R.layout.main);
      Uri uri;
      if (mParents.empty()) {
        uri = ProviderTableMeta.CONTENT_URI;
      } else {
        uri = Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR, mParents.peek());
      }
      mCursor = managedQuery(uri,
                             null,
                             ProviderTableMeta.FILE_ACCOUNT_OWNER+"=?",
                             new String[]{mAccount.name}, null);
  
      if (mCursor.moveToFirst()) {
        TextView tv = (TextView) findViewById(R.id.directory_name);
        tv.setText(mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_PATH)));
      }
      getListView().setAdapter(new FileListListAdapter(mCursor, this));      
      getListView().invalidate();
      return;
    }
    if (mParents.size()==0) {
      super.onBackPressed();
      return;
    } else if (mParents.size() == 1) {
      mParents.pop();
      mCursor = managedQuery(ProviderTableMeta.CONTENT_URI,
          null,
          ProviderTableMeta.FILE_ACCOUNT_OWNER+"=?",
          new String[]{mAccount.name},
          null);
    } else {
      mParents.pop();
      mCursor = managedQuery(Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR, mParents.peek()),
          null,
          ProviderTableMeta.FILE_ACCOUNT_OWNER+"=?",
          new String[]{mAccount.name},
          null);
    }
    
    setListAdapter(new FileListListAdapter(mCursor, this));
    getListView().invalidate();

    TextView tv = (TextView) findViewById(R.id.directory_name);
    String s = tv.getText().toString();
    if (s.endsWith("/")) {
      s = s.substring(0, s.length() - 1);
    }
    s = s.substring(0, s.lastIndexOf('/') + 1);
    tv.setText(s);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    if (!mCursor.moveToPosition(position)) {
      throw new IndexOutOfBoundsException("Incorrect item selected");
    }
    if (!mIsDisplayingFile) {
      if (mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE)).equals("DIR")) {
        String id_ = mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta._ID));
        String dirname = mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_NAME));
        TextView tv = (TextView) findViewById(R.id.directory_name);
        tv.setText(tv.getText().toString()+dirname+"/");
        mParents.push(id_);
        mCursor = managedQuery(Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR, id_),
                               null,
                               ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                               new String[]{mAccount.name}, null);
        setListAdapter(new FileListListAdapter(mCursor, this));
      } else {
        mIsDisplayingFile = true;
        setContentView(R.layout.file_display);
        String id_ = mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta._ID));
        mCursor = managedQuery(Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_FILE, id_),
                               null,
                               null,
                               null,
                               null);
        mCursor.moveToFirst();
        // filename
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setText(mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_NAME)));
        // filetype
        tv = (TextView) findViewById(R.id.textView2);
        tv.setText(DisplayUtils.convertMIMEtoPrettyPrint(mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE))));
        // size
        tv = (TextView) findViewById(R.id.textView3);
        tv.setText(DisplayUtils.bitsToHumanReadable(mCursor.getLong(mCursor.getColumnIndex(ProviderTableMeta.FILE_CONTENT_LENGTH))));
        // modified
        tv = (TextView) findViewById(R.id.textView4);
        tv.setText(mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_MODIFIED)));
        if (!TextUtils.isEmpty(mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH)))) {
          ImageView iv = (ImageView) findViewById(R.id.imageView1);
          Bitmap bmp = BitmapFactory.decodeFile(mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH)));
          Matrix m = new Matrix();
          float scale;
          if (bmp.getWidth() > bmp.getHeight()) {
            scale = (float) (200./bmp.getWidth());
          } else {
            scale = (float) (200./bmp.getHeight());
          }
          m.postScale(scale, scale);
          Bitmap newBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
          iv.setImageBitmap(newBmp);
        }
        setListAdapter(new FileListActionListAdapter(mCursor, this, mAccount));
      }    
      getListView().invalidate();
    } else {
      try {
        Intent i = (Intent) getListAdapter().getItem(position);
        startActivity(i);
      } catch (ClassCastException e) {}
    }
  }
  
  private void populateFileList() {
    TextView tv = (TextView) findViewById(R.id.directory_name);
    tv.setText("/");
    mCursor = getContentResolver().query(ProviderTableMeta.CONTENT_URI,
                                         null,
                                         ProviderTableMeta.FILE_ACCOUNT_OWNER+"=?",
                                         new String[]{mAccount.name},
                                         null);
    setListAdapter(new FileListListAdapter(mCursor, this));
    getListView().invalidate();
  }
  
  

}