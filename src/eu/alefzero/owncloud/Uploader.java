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
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Stack;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.db.ProviderMeta;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

/**
 * This can be used to upload things to an ownCloud instance.
 * @author Bartek Przybylski
 *
 */
public class Uploader extends ListActivity implements OnItemClickListener, android.view.View.OnClickListener {
  private static final String TAG = "ownCloudUploader";

  private Account mAccount;
  private AccountManager mAccountManager;
  private String mUsername, mPassword;
  private Cursor mCursor;
  private Stack<String> mParents;
  private Thread mUploadThread;
  private Handler mHandler;
  private ArrayList<Parcelable> mStreamsToUpload;

  private final static int DIALOG_NO_ACCOUNT = 0;
  private final static int DIALOG_WAITING = 1;
  private final static int DIALOG_NO_STREAM = 2;
  private final static int DIALOG_MULTIPLE_ACCOUNT = 3;
  private final static int DIALOG_GET_DIRNAME = 4;

  private final static int REQUEST_CODE_SETUP_ACCOUNT = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    mParents = new Stack<String>();
    mHandler = new Handler();
    if (getIntent().hasExtra(Intent.EXTRA_STREAM)) {
      prepareStreamsToUpload();
      mAccountManager = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
      Account[] accounts = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
      if (accounts.length == 0) {
        Log.i(TAG, "No ownCloud account is available");
        showDialog(DIALOG_NO_ACCOUNT);
      } else if (accounts.length > 1) {
        Log.i(TAG, "More then one ownCloud is available");
        showDialog(DIALOG_MULTIPLE_ACCOUNT);
      } else {
        mAccount = accounts[0];
        setContentView(R.layout.uploader_layout);
        populateDirectoryList();
      }
    } else {
      showDialog(DIALOG_NO_STREAM);
    }
  }

  @Override
  protected Dialog onCreateDialog(final int id) {
    final AlertDialog.Builder builder = new Builder(this);
    switch (id) {
      case DIALOG_WAITING:
        ProgressDialog pDialog = new ProgressDialog(this);
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
        pDialog.setMessage(getResources().getString(R.string.uploader_info_uploading));
        return pDialog;
      case DIALOG_NO_ACCOUNT:
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setTitle(R.string.uploader_wrn_no_account_title);
        builder.setMessage(R.string.uploader_wrn_no_account_text);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.uploader_wrn_no_account_setup_btn_text, new OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.ECLAIR_MR1) {
              // using string value since in API7 this constatn is not defined
              // in API7 < this constatant is defined in Settings.ADD_ACCOUNT_SETTINGS
              // and Settings.EXTRA_AUTHORITIES
              Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
              intent.putExtra("authorities", new String[] {AccountAuthenticator.AUTH_TOKEN_TYPE});
              startActivityForResult(intent, REQUEST_CODE_SETUP_ACCOUNT);
            } else {
              // since in API7 there is no direct call for account setup, so we need to
              // show our own AccountSetupAcricity, get desired results and setup
              // everything for ourself
              Intent intent = new Intent(getBaseContext(), AccountAuthenticator.class);
              startActivityForResult(intent, REQUEST_CODE_SETUP_ACCOUNT);
            }
          }
        });
        builder.setNegativeButton(R.string.uploader_wrn_no_account_quit_btn_text, new OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            finish();
          }
        });
        return builder.create();
      case DIALOG_GET_DIRNAME:
        final EditText dirName = new EditText(getBaseContext());
        builder.setView(dirName);
        builder.setTitle(R.string.uploader_info_dirname);
        String pathToUpload;
        if (mParents.empty()) {
          pathToUpload = "/";
        } else {
          mCursor = managedQuery(Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_FILE, mParents.peek()), 
                                 null,
                                 null,
                                 null,
                                 null);
          mCursor.moveToFirst();
          pathToUpload = mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_PATH)) +
                         mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_NAME)).replace(" ", "%20");
        }
        a a = new a(pathToUpload, dirName);
        builder.setPositiveButton(R.string.common_ok, a);
        builder.setNegativeButton(R.string.common_cancel, new OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
          }
        });
        return builder.create();
      case DIALOG_MULTIPLE_ACCOUNT:
        CharSequence ac[] = new CharSequence[mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE).length];
        for (int i = 0;  i < ac.length; ++i) {
          ac[i] = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)[i].name;
        }
        builder.setTitle(R.string.common_choose_account);
        builder.setItems(ac, new OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            mAccount = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)[which];
            populateDirectoryList();
          }
        });
        builder.setCancelable(true);
        builder.setOnCancelListener(new OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
            dialog.cancel();
            finish();
          }
        });
        return builder.create();
      default:
        throw new IllegalArgumentException("Unknown dialog id: " + id);
    }
  }
  
  class a implements OnClickListener {
    String mPath;
    EditText mDirname;
    public a(String path, EditText dirname) {
      mPath = path; mDirname = dirname;
    }
    public void onClick(DialogInterface dialog, int which) {
      showDialog(DIALOG_WAITING);
      mUploadThread = new Thread(new BackgroundUploader(mPath+mDirname.getText().toString(), mStreamsToUpload, mHandler, true));
      mUploadThread.start();
    }
  }
  
  @Override
  public void onBackPressed() {
    
    if (mParents.size()==0) {
      super.onBackPressed();
      return;
    } else if (mParents.size() == 1) {
      mParents.pop();
      mCursor = managedQuery(ProviderTableMeta.CONTENT_URI,
          null,
          ProviderTableMeta.FILE_CONTENT_TYPE+"=?",
          new String[]{"DIR"},
          null);
    } else {
      mParents.pop();
      mCursor = managedQuery(Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR, mParents.peek()),
          null,
          ProviderTableMeta.FILE_CONTENT_TYPE+"=?",
          new String[]{"DIR"},
          null);
    }
    
    SimpleCursorAdapter sca = new SimpleCursorAdapter(this, R.layout.uploader_list_item_layout,
                                   mCursor,
                                   new String[]{ProviderTableMeta.FILE_NAME},
                                   new int[]{R.id.textView1});
    setListAdapter(sca);
  }
  
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    if (!mCursor.moveToPosition(position)) {
      throw new IndexOutOfBoundsException("Incorrect item selected");
    }
    String _id = mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta._ID));
    mParents.push(_id);
    
    mCursor.close();
    mCursor = managedQuery(Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR, _id),
                           null,
                           ProviderTableMeta.FILE_CONTENT_TYPE+"=?",
                           new String[]{"DIR"},
                           null);
    SimpleCursorAdapter sca = new SimpleCursorAdapter(this, R.layout.uploader_list_item_layout,
                                                      mCursor,
                                                      new String[]{ProviderTableMeta.FILE_NAME},
                                                      new int[]{R.id.textView1});
    setListAdapter(sca);
    getListView().invalidate();
  }

  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.uploader_choose_folder:
        String pathToUpload = null;
        if (mParents.empty()) {
          pathToUpload = "/";
        } else {
          mCursor = managedQuery(Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_FILE, mParents.peek()), 
                                 null,
                                 null,
                                 null,
                                 null);
          mCursor.moveToFirst();
          pathToUpload = mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_PATH)) +
                         mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_NAME)).replace(" ", "%20");
        }
        
        showDialog(DIALOG_WAITING);
        mUploadThread = new Thread(new BackgroundUploader(pathToUpload, mStreamsToUpload, mHandler));
        mUploadThread.start();
        
        break;
      case android.R.id.button1: // dynamic action for create aditional dir
        showDialog(DIALOG_GET_DIRNAME);
        break;
      default:
        throw new IllegalArgumentException("Wrong element clicked");
    }
  }

  public void onUploadComplete(boolean uploadSucc, String message) {
    dismissDialog(DIALOG_WAITING);
    Log.i(TAG, "UploadSucc: " + uploadSucc + " message: " + message);
    if (uploadSucc) {
      Toast.makeText(this, getResources().getString(R.string.uploader_upload_succeed), Toast.LENGTH_SHORT).show();
    } else {
      Toast.makeText(this, getResources().getString(R.string.uploader_upload_failed) + message, Toast.LENGTH_LONG).show();
    }
    finish();
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Log.i(TAG, "result received. req: " + requestCode + " res: " + resultCode);
    if (requestCode == REQUEST_CODE_SETUP_ACCOUNT) {
      dismissDialog(DIALOG_NO_ACCOUNT);
      if (resultCode == RESULT_CANCELED) {
        finish();
      }
      Account[] accounts = mAccountManager.getAccountsByType(AccountAuthenticator.AUTH_TOKEN_TYPE);
      if (accounts.length == 0) {
        showDialog(DIALOG_NO_ACCOUNT);
      } else {
        // there is no need for checking for is there more then one account at this point
        // since account setup can set only one account at time
        mAccount = accounts[0];
        populateDirectoryList();
      }
    }
  }
  
  private void populateDirectoryList() {
    mUsername = mAccount.name.substring(0, mAccount.name.indexOf('@'));
    mPassword = mAccountManager.getPassword(mAccount);
    setContentView(R.layout.uploader_layout);
    mCursor = managedQuery(ProviderMeta.ProviderTableMeta.CONTENT_URI,
                           null,
                           ProviderTableMeta.FILE_CONTENT_TYPE+"=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                           new String[]{"DIR", mAccount.name},
                           null);

    ListView lv = getListView();
    lv.setOnItemClickListener(this);
    SimpleCursorAdapter sca = new SimpleCursorAdapter(this,
                                                      R.layout.uploader_list_item_layout,
                                                      mCursor,
                                                      new String[]{ProviderTableMeta.FILE_NAME},
                                                      new int[]{R.id.textView1});
    setListAdapter(sca);
    Button btn = (Button) findViewById(R.id.uploader_choose_folder);
    btn.setOnClickListener(this);
    // insert create new directory for multiple items uploading
    if (getIntent().getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
      Button createDirBtn = new Button(this);
      createDirBtn.setId(android.R.id.button1);
      createDirBtn.setText(R.string.uploader_btn_create_dir_text);
      createDirBtn.setOnClickListener(this);
      ((LinearLayout)findViewById(R.id.linearLayout1)).addView(createDirBtn, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
    }
  }
  
  private void prepareStreamsToUpload() {
    if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
      mStreamsToUpload = new ArrayList<Parcelable>();
      mStreamsToUpload.add(getIntent().getParcelableExtra(Intent.EXTRA_STREAM));
    } else if (getIntent().getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
      mStreamsToUpload = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
    } else {
      // unknow action inserted
      throw new IllegalArgumentException("Unknown action given: " + getIntent().getAction());
    }
  }
  
  public void PartialupdateUpload(String fileLocalPath, String filename, String filepath, String contentType, String contentLength) {
    ContentValues cv = new ContentValues();
    cv.put(ProviderTableMeta.FILE_NAME, filename);
    cv.put(ProviderTableMeta.FILE_PATH, filepath);
    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, fileLocalPath);
    cv.put(ProviderTableMeta.FILE_MODIFIED, WebdavUtils.DISPLAY_DATE_FORMAT.format(new java.util.Date()));
    cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, contentType);
    cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, contentLength);
    cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, mAccount.name);
    Log.d(TAG, filename+" ++ "+filepath+" ++ " + contentLength + " ++ " + contentType + " ++ " + fileLocalPath);
    if (!mParents.empty()) {
      Cursor c = managedQuery(Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_FILE, mParents.peek()),
                              null,
                              null,
                              null,
                              null);
      c.moveToFirst();
      cv.put(ProviderTableMeta.FILE_PARENT, c.getString(c.getColumnIndex(ProviderTableMeta._ID)));
      c.close();
    }
    getContentResolver().insert(ProviderTableMeta.CONTENT_URI_FILE, cv);
  }
  
  class BackgroundUploader implements Runnable {
    private ArrayList<Parcelable> mUploadStreams;
    private Handler mHandler;
    private String mUploadPath;
    private boolean mCreateDir;
    
    public BackgroundUploader(String pathToUpload, ArrayList<Parcelable> streamsToUpload,
        Handler handler) {
      mUploadStreams = streamsToUpload;
      mHandler = handler;
      mUploadPath = pathToUpload.replace(" ", "%20");
      mCreateDir = false;
    }

    public BackgroundUploader(String pathToUpload, ArrayList<Parcelable> streamsToUpload,
                              Handler handler, boolean createDir) {
      mUploadStreams = streamsToUpload;
      mHandler = handler;
      mUploadPath = pathToUpload.replace(" ", "%20");
      mCreateDir = createDir;
    }

    public void run() {
      WebdavClient wdc = new WebdavClient(Uri.parse(mAccountManager.getUserData(mAccount,
          AccountAuthenticator.KEY_OC_URL)));
      wdc.setCredentials(mUsername, mPassword);
      wdc.allowUnsignedCertificates();

      // create last directory in path if nessesary
      if (mCreateDir) {
        wdc.createDirectory(mUploadPath);
      }
      
      for (int i = 0; i < mUploadStreams.size(); ++i) {
        Uri uri = (Uri) mUploadStreams.get(i);
        if (uri.getScheme().equals("content")) {
          final Cursor c = getContentResolver().query((Uri) mUploadStreams.get(i), null, null, null, null);
          c.moveToFirst();
          
          if (!wdc.putFile(c.getString(c.getColumnIndex(Media.DATA)),
                           mUploadPath+"/"+c.getString(c.getColumnIndex(Media.DISPLAY_NAME)),
                           c.getString(c.getColumnIndex(Media.MIME_TYPE)))) {
            mHandler.post(new Runnable() {
              public void run() {
                Uploader.this.onUploadComplete(false, "Error while uploading file: " + c.getString(c.getColumnIndex(Media.DISPLAY_NAME)));
              }
            });
          } else {
            mHandler.post(new Runnable() {
              public void run() {
                Uploader.this.PartialupdateUpload(c.getString(c.getColumnIndex(Media.DATA)),
                    c.getString(c.getColumnIndex(Media.DISPLAY_NAME)),
                    mUploadPath+"/"+c.getString(c.getColumnIndex(Media.DISPLAY_NAME)),
                    c.getString(c.getColumnIndex(Media.MIME_TYPE)),
                    c.getString(c.getColumnIndex(Media.SIZE)));
              }
            });
          }
        } else if (uri.getScheme().equals("file")) {
         final File file = new File(Uri.decode(uri.toString()).replace(uri.getScheme()+"://", ""));
         FileNameMap fileNameMap = URLConnection.getFileNameMap();
         String contentType = fileNameMap.getContentTypeFor(uri.toString());
         if (contentType == null) {
           contentType = "text/plain";
         }
         if (!wdc.putFile(file.getAbsolutePath(), mUploadPath+"/"+file.getName(), contentType)) {
           mHandler.post(new Runnable() {
             public void run() {
               Uploader.this.onUploadComplete(false, "Error while uploading file: " + file.getName());
             }
           });
         }
        }
        
      }
      mHandler.post(new Runnable() {
        public void run() {
          Uploader.this.onUploadComplete(true, null);
        }
      });
    }

  }
  
}
