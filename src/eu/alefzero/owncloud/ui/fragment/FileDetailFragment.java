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
package eu.alefzero.owncloud.ui.fragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity.Header;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;

import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.DisplayUtils;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.datamodel.FileDataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.files.services.FileDownloader;
import eu.alefzero.owncloud.utils.OwnCloudVersion;
import eu.alefzero.webdav.WebdavClient;

/**
 * This Fragment is used to display the details about a file.
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileDetailFragment extends SherlockFragment implements
        OnClickListener {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private int mLayout;
    private View mView;
    private OCFile mFile;
    private Account mAccount;
    
    private DownloadFinishReceiver mDownloadFinishReceiver;

    private static final String TAG = "FileDetailFragment";
    public static final String FTAG = "FileDetails"; 

    
    /**
     * Creates an empty details fragment.
     * 
     * It's necessary to keep a public constructor without parameters; the system uses it when tries to reinstantiate a fragment automatically. 
     */
    public FileDetailFragment() {
        mFile = null;
        mAccount = null;
        mLayout = R.layout.file_details_empty;
    }
    
    
    /**
     * Creates a details fragment.
     * 
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     * 
     * @param fileToDetail      An {@link OCFile} to show in the fragment
     * @param ocAccount         An ownCloud account; needed to start downloads
     */
    public FileDetailFragment(OCFile fileToDetail, Account ocAccount){
        mFile = fileToDetail;
        mAccount = ocAccount;
        mLayout = R.layout.file_details_empty;
        
        if(fileToDetail != null && ocAccount != null) {
            mLayout = R.layout.file_details_fragment;
        }
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(FileDetailFragment.EXTRA_FILE);
            mAccount = savedInstanceState.getParcelable(FileDetailFragment.EXTRA_ACCOUNT);
        }
        
        View view = null;
        view = inflater.inflate(mLayout, container, false);
        mView = view;
        
        updateFileDetails();
        return view;
    }
    

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(getClass().toString(), "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDetailFragment.EXTRA_FILE, mFile);
        outState.putParcelable(FileDetailFragment.EXTRA_ACCOUNT, mAccount);
        Log.i(getClass().toString(), "onSaveInstanceState() end");
    }

    
    @Override
    public void onResume() {
        super.onResume();
        mDownloadFinishReceiver = new DownloadFinishReceiver();
        IntentFilter filter = new IntentFilter(
                FileDownloader.DOWNLOAD_FINISH_MESSAGE);
        getActivity().registerReceiver(mDownloadFinishReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mDownloadFinishReceiver);
        mDownloadFinishReceiver = null;
    }

    @Override
    public View getView() {
        return super.getView() == null ? mView : super.getView();
    }

    
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fdDownloadBtn: {
                //Toast.makeText(getActivity(), "Downloading", Toast.LENGTH_LONG).show();
                Intent i = new Intent(getActivity(), FileDownloader.class);
                i.putExtra(FileDownloader.EXTRA_ACCOUNT, mAccount);
                i.putExtra(FileDownloader.EXTRA_REMOTE_PATH, mFile.getRemotePath());
                i.putExtra(FileDownloader.EXTRA_FILE_PATH, mFile.getURLDecodedRemotePath());
                i.putExtra(FileDownloader.EXTRA_FILE_SIZE, mFile.getFileLength());
                v.setEnabled(false);
                getActivity().startService(i);
                break;
            }
            case R.id.fdKeepInSync: {
                CheckBox cb = (CheckBox) getView().findViewById(R.id.fdKeepInSync);
                mFile.setKeepInSync(cb.isChecked());
                FileDataStorageManager fdsm = new FileDataStorageManager(mAccount, getActivity().getApplicationContext().getContentResolver());
                fdsm.saveFile(mFile);
                if (mFile.keepInSync() && !mFile.isDownloaded()) {
                    onClick(getView().findViewById(R.id.fdDownloadBtn));
                }
                break;
            }
            case R.id.fdRenameBtn: {
                EditNameFragment dialog = EditNameFragment.newInstance(mFile.getFileName());
                dialog.show(getFragmentManager(), "nameeditdialog");
                dialog.setOnDismissListener(this);
                break;
            }
            default:
                Log.e(TAG, "Incorrect view clicked!");
        }
        
        /* else if (v.getId() == R.id.fdShareBtn) {
            Thread t = new Thread(new ShareRunnable(mFile.getRemotePath()));
            t.start();
        }*/
    }


    /**
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be replaced.
     * 
     * @return  True when the fragment was created with the empty layout.
     */
    public boolean isEmpty() {
        return mLayout == R.layout.file_details_empty;
    }

    
    /**
     * Can be used to get the file that is currently being displayed.
     * @return The file on the screen.
     */
    public OCFile getDisplayedFile(){
        return mFile;
    }
    
    /**
     * Use this method to signal this Activity that it shall update its view.
     * 
     * @param file : An {@link OCFile}
     */
    public void updateFileDetails(OCFile file, Account ocAccount) {
        mFile = file;
        mAccount = ocAccount;
        updateFileDetails();
    }
    

    /**
     * Updates the view with all relevant details about that file.
     */
    public void updateFileDetails() {

        if (mFile != null && mAccount != null && mLayout == R.layout.file_details_fragment) {
            
            Button downloadButton = (Button) getView().findViewById(R.id.fdDownloadBtn);
            // set file details
            setFilename(mFile.getFileName());
            setFiletype(DisplayUtils.convertMIMEtoPrettyPrint(mFile
                    .getMimetype()));
            setFilesize(mFile.getFileLength());
            if(ocVersionSupportsTimeCreated()){
                setTimeCreated(mFile.getCreationTimestamp());
            }
           
            setTimeModified(mFile.getModificationTimestamp());
            
            CheckBox cb = (CheckBox)getView().findViewById(R.id.fdKeepInSync);
            cb.setChecked(mFile.keepInSync());
            cb.setOnClickListener(this);
            //getView().findViewById(R.id.fdShareBtn).setOnClickListener(this);
            getView().findViewById(R.id.fdRenameBtn).setOnClickListener(this);
            
            if (mFile.getStoragePath() != null) {
                // Update preview
                ImageView preview = (ImageView) getView().findViewById(R.id.fdPreview);
                try {
                    if (mFile.getMimetype().startsWith("image/")) {
                        BitmapFactory.Options options = new Options();
                        options.inScaled = true;
                        options.inPurgeable = true;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
                            options.inPreferQualityOverSpeed = false;
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                            options.inMutable = false;
                        }

                        Bitmap bmp = BitmapFactory.decodeFile(mFile.getStoragePath(), options);

                        if (bmp != null) {
                            int width = options.outWidth;
                            int height = options.outHeight;
                            int scale = 1;
                            boolean recycle = false;
                            if (width >= 2048 || height >= 2048) {
                                scale = (int) (Math.ceil(Math.max(height, width)/2048.));
                                options.inSampleSize = scale;
                                recycle = true;
                            }
                                Display display = getActivity().getWindowManager().getDefaultDisplay();
                                Point size = new Point();
                                int screenwidth;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
                                    display.getSize(size);
                                    screenwidth = size.x;
                                } else {
                                    screenwidth = display.getWidth();
                                }

                                Log.e("ASD", "W " + width + " SW " + screenwidth);

                                if (width > screenwidth) {
                                    scale = (int) (Math.ceil(Math.max(height, width)/screenwidth));
                                    options.inSampleSize = scale;
                                    recycle = true;
                                }
                            

                            if (recycle) bmp.recycle();
                            bmp = BitmapFactory.decodeFile(mFile.getStoragePath(), options);
                            
                        }
                        if (bmp != null) {
                            preview.setImageBitmap(bmp);
                        }
                    }
                } catch (OutOfMemoryError e) {
                    preview.setVisibility(View.INVISIBLE);
                    Log.e(TAG, "Out of memory occured for file with size " + mFile.getFileLength());
                    
                } catch (NoSuchFieldError e) {
                    preview.setVisibility(View.INVISIBLE);
                    Log.e(TAG, "Error from access to unexisting field despite protection " + mFile.getFileLength());
                    
                } catch (Throwable t) {
                    preview.setVisibility(View.INVISIBLE);
                    Log.e(TAG, "Unexpected error while creating image preview " + mFile.getFileLength(), t);
                }
                
                // Change download button to open button
                downloadButton.setText(R.string.filedetails_open);
                downloadButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String storagePath = mFile.getStoragePath();
                        try {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setDataAndType(Uri.parse("file://"+ storagePath), mFile.getMimetype());
                            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivity(i);
                            
                        } catch (Throwable t) {
                            Log.e(TAG, "Fail when trying to open with the mimeType provided from the ownCloud server: " + mFile.getMimetype());
                            boolean toastIt = true; 
                            String mimeType = "";
                            try {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(storagePath.substring(storagePath.lastIndexOf('.') + 1));
                                if (mimeType != null && !mimeType.equals(mFile.getMimetype())) {
                                    i.setDataAndType(Uri.parse("file://"+mFile.getStoragePath()), mimeType);
                                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    startActivity(i);
                                    toastIt = false;
                                }
                                
                            } catch (IndexOutOfBoundsException e) {
                                Log.e(TAG, "Trying to find out MIME type of a file without extension: " + storagePath);
                                
                            } catch (ActivityNotFoundException e) {
                                Log.e(TAG, "No activity found to handle: " + storagePath + " with MIME type " + mimeType + " obtained from extension");
                                
                            } catch (Throwable th) {
                                Log.e(TAG, "Unexpected problem when opening: " + storagePath, th);
                                
                            } finally {
                                if (toastIt) {
                                    Toast.makeText(getActivity(), "There is no application to handle file " + mFile.getFileName(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            
                        }
                    }
                });
            } else {
                // Make download button effective
                downloadButton.setOnClickListener(this);
            }
        }
    }
    
    
    /**
     * Updates the filename in view
     * @param filename to set
     */
    private void setFilename(String filename) {
        TextView tv = (TextView) getView().findViewById(R.id.fdFilename);
        if (tv != null)
            tv.setText(filename);
    }

    /**
     * Updates the MIME type in view
     * @param mimetype to set
     */
    private void setFiletype(String mimetype) {
        TextView tv = (TextView) getView().findViewById(R.id.fdType);
        if (tv != null)
            tv.setText(mimetype);
    }

    /**
     * Updates the file size in view
     * @param filesize in bytes to set
     */
    private void setFilesize(long filesize) {
        TextView tv = (TextView) getView().findViewById(R.id.fdSize);
        if (tv != null)
            tv.setText(DisplayUtils.bytesToHumanReadable(filesize));
    }
    
    /**
     * Updates the time that the file was created in view
     * @param milliseconds Unix time to set
     */
    private void setTimeCreated(long milliseconds){
        TextView tv = (TextView) getView().findViewById(R.id.fdCreated);
        TextView tvLabel = (TextView) getView().findViewById(R.id.fdCreatedLabel);
        if(tv != null){
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
            tv.setVisibility(View.VISIBLE);
            tvLabel.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Updates the time that the file was last modified
     * @param milliseconds Unix time to set
     */
    private void setTimeModified(long milliseconds){
        TextView tv = (TextView) getView().findViewById(R.id.fdModified);
        if(tv != null){
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
        }
    }
    
    /**
     * In ownCloud 3.X.X and 4.X.X there is a bug that SabreDAV does not return
     * the time that the file was created. There is a chance that this will
     * be fixed in future versions. Use this method to check if this version of
     * ownCloud has this fix.
     * @return True, if ownCloud the ownCloud version is supporting creation time
     */
    private boolean ocVersionSupportsTimeCreated(){
        /*if(mAccount != null){
            AccountManager accManager = (AccountManager) getActivity().getSystemService(Context.ACCOUNT_SERVICE);
            OwnCloudVersion ocVersion = new OwnCloudVersion(accManager
                    .getUserData(mAccount, AccountAuthenticator.KEY_OC_VERSION));
            if(ocVersion.compareTo(new OwnCloudVersion(0x030000)) < 0) {
                return true;
            }
        }*/
        return false;
    }

    /**
     * Once the file download has finished -> update view
     * @author Bartek Przybylski
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getView()!=null && getView().findViewById(R.id.fdDownloadBtn) != null) 
                getView().findViewById(R.id.fdDownloadBtn).setEnabled(true);
            
            if (intent.getBooleanExtra(FileDownloader.EXTRA_DOWNLOAD_RESULT, false)) {
                mFile.setStoragePath(intent.getStringExtra(FileDownloader.EXTRA_FILE_PATH));
                updateFileDetails();
            } else if (intent.getAction().equals(FileDownloader.DOWNLOAD_FINISH_MESSAGE)) {
                Toast.makeText(context, R.string.downloader_download_failed , Toast.LENGTH_SHORT).show();
            }
        }
        
    }
    
    // this is a temporary class for sharing purposes, it need to be replacead in transfer service
    private class ShareRunnable implements Runnable {
        private String mPath;

        public ShareRunnable(String path) {
            mPath = path;
        }
        
        public void run() {
            AccountManager am = AccountManager.get(getActivity());
            Account account = AccountUtils.getCurrentOwnCloudAccount(getActivity());
            OwnCloudVersion ocv = new OwnCloudVersion(am.getUserData(account, AccountAuthenticator.KEY_OC_VERSION));
            String url = am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL) + AccountUtils.getWebdavPath(ocv);

            Log.d("share", "sharing for version " + ocv.toString());

            if (ocv.compareTo(new OwnCloudVersion(0x040000)) >= 0) {
                String APPS_PATH = "/apps/files_sharing/";
                String SHARE_PATH = "ajax/share.php";

                String SHARED_PATH = "/apps/files_sharing/get.php?token=";
                
                final String WEBDAV_SCRIPT = "webdav.php";
                final String WEBDAV_FILES_LOCATION = "/files/";
                
                WebdavClient wc = new WebdavClient();
                HttpConnectionManagerParams params = new HttpConnectionManagerParams();
                params.setMaxConnectionsPerHost(wc.getHostConfiguration(), 5);

                //wc.getParams().setParameter("http.protocol.single-cookie-header", true);
                //wc.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);

                PostMethod post = new PostMethod(am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL) + APPS_PATH + SHARE_PATH);

                post.addRequestHeader("Content-type","application/x-www-form-urlencoded; charset=UTF-8" );
                post.addRequestHeader("Referer", am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL));
                List<NameValuePair> formparams = new ArrayList<NameValuePair>();
                Log.d("share", mPath+"");
                formparams.add(new BasicNameValuePair("sources",mPath));
                formparams.add(new BasicNameValuePair("uid_shared_with", "public"));
                formparams.add(new BasicNameValuePair("permissions", "0"));
                post.setRequestEntity(new StringRequestEntity(URLEncodedUtils.format(formparams, HTTP.UTF_8)));

                int status;
                try {
                    PropFindMethod find = new PropFindMethod(url+"/");
                    find.addRequestHeader("Referer", am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL));
                    Log.d("sharer", ""+ url+"/");
                    wc.setCredentials(account.name.substring(0, account.name.lastIndexOf('@')), am.getPassword(account));
                    
                    for (org.apache.commons.httpclient.Header a : find.getRequestHeaders()) {
                        Log.d("sharer-h", a.getName() + ":"+a.getValue());
                    }
                    
                    int status2 = wc.executeMethod(find);

                    Log.d("sharer", "propstatus "+status2);
                    
                    GetMethod get = new GetMethod(am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL) + "/");
                    get.addRequestHeader("Referer", am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL));
                    
                    status2 = wc.executeMethod(get);

                    Log.d("sharer", "getstatus "+status2);
                    Log.d("sharer", "" + get.getResponseBodyAsString());
                    
                    for (org.apache.commons.httpclient.Header a : get.getResponseHeaders()) {
                        Log.d("sharer", a.getName() + ":"+a.getValue());
                    }

                    status = wc.executeMethod(post);
                    for (org.apache.commons.httpclient.Header a : post.getRequestHeaders()) {
                        Log.d("sharer-h", a.getName() + ":"+a.getValue());
                    }
                    for (org.apache.commons.httpclient.Header a : post.getResponseHeaders()) {
                        Log.d("sharer", a.getName() + ":"+a.getValue());
                    }
                    String resp = post.getResponseBodyAsString();
                    Log.d("share", ""+post.getURI().toString());
                    Log.d("share", "returned status " + status);
                    Log.d("share", " " +resp);
                    
                    if(status != HttpStatus.SC_OK ||resp == null || resp.equals("") || resp.startsWith("false")) {
                        return;
                     }

                    JSONObject jsonObject = new JSONObject (resp);
                    String jsonStatus = jsonObject.getString("status");
                    if(!jsonStatus.equals("success")) throw new Exception("Error while sharing file status != success");
                    
                    String token = jsonObject.getString("data");
                    String uri = am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL) + SHARED_PATH + token; 
                    Log.d("Actions:shareFile ok", "url: " + uri);   
                    
                } catch (HttpException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            } else if (ocv.compareTo(new OwnCloudVersion(0x030000)) >= 0) {
                
            }
        }
    }
    
    public void onDismiss(EditNameFragment dialog) {
        Log.e("ASD","ondismiss");
        if (dialog instanceof EditNameFragment) {
            if (((EditNameFragment)dialog).getResult()) {
                String newFilename = ((EditNameFragment)dialog).getNewFilename();
                Log.d(TAG, "name edit dialog dismissed with new name " + newFilename);
                if (!newFilename.equals(mFile.getFileName())) {
                    FileDataStorageManager fdsm = new FileDataStorageManager(mAccount, getActivity().getContentResolver());
                    if (fdsm.getFileById(mFile.getFileId()) != null) {
                        OCFile newFile = new OCFile(fdsm.getFileById(mFile.getParentId()).getRemotePath()+"/"+newFilename);
                        newFile.setCreationTimestamp(mFile.getCreationTimestamp());
                        newFile.setFileId(mFile.getFileId());
                        newFile.setFileLength(mFile.getFileLength());
                        newFile.setKeepInSync(mFile.keepInSync());
                        newFile.setLastSyncDate(mFile.getLastSyncDate());
                        newFile.setMimetype(mFile.getMimetype());
                        newFile.setModificationTimestamp(mFile.getModificationTimestamp());
                        newFile.setParentId(mFile.getParentId());
                        newFile.setStoragePath(mFile.getStoragePath());
                        fdsm.removeFile(mFile);
                        fdsm.saveFile(newFile);
                        new Thread(new RenameRunnable(mFile, newFile, mAccount)).start();
                        mFile = newFile;
                        updateFileDetails(mFile, mAccount);
                    }
                }
            }
        } else {
            Log.e(TAG, "Unknown dialog intance passed to onDismissDalog: " + dialog.getClass().getCanonicalName());
        }
        
    }
    
    private class RenameRunnable implements Runnable {
        
        Account mAccount;
        OCFile mOld, mNew;
        
        public RenameRunnable(OCFile oldFile, OCFile newFile, Account account) {
            mOld = oldFile;
            mNew = newFile;
            mAccount = account;
        }
        
        public void run() {
            WebdavClient wc = new WebdavClient(mAccount, getSherlockActivity().getApplicationContext());
            AccountManager am = AccountManager.get(getSherlockActivity());
            String baseUrl = am.getUserData(mAccount, AccountAuthenticator.KEY_OC_BASE_URL);
            OwnCloudVersion ocv = new OwnCloudVersion(am.getUserData(mAccount, AccountAuthenticator.KEY_OC_VERSION));
            String webdav_path = AccountUtils.getWebdavPath(ocv);
            Log.d("ASD", ""+baseUrl + webdav_path + mOld.getRemotePath());

            
            Log.e("ASD", Uri.parse(baseUrl).getPath() == null ? "" : Uri.parse(baseUrl).getPath() + webdav_path + mNew.getRemotePath());
            LocalMoveMethod move = new LocalMoveMethod(baseUrl + webdav_path + mOld.getRemotePath(),
                                             Uri.parse(baseUrl).getPath() == null ? "" : Uri.parse(baseUrl).getPath() + webdav_path + mNew.getRemotePath());
            
            try {
                int status = wc.executeMethod(move);
                Log.e("ASD", ""+move.getQueryString());
                Log.d("move", "returned status " + status);
            } catch (HttpException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        private class LocalMoveMethod extends DavMethodBase {

            public LocalMoveMethod(String uri, String dest) {
                super(uri);
                addRequestHeader(new org.apache.commons.httpclient.Header("Destination", dest));
            }

            @Override
            public String getName() {
                return "MOVE";
            }

            @Override
            protected boolean isSuccess(int status) {
                return status == 201 || status == 204;
            }
            
        }
    }
    
    private static class EditNameFragment extends SherlockDialogFragment implements OnClickListener {

        private String mNewFilename;
        private boolean mResult;
        private FileDetailFragment mListener;
        
        static public EditNameFragment newInstance(String filename) {
            EditNameFragment f = new EditNameFragment();
            Bundle args = new Bundle();
            args.putString("filename", filename);
            f.setArguments(args);
            return f;
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.edit_box_dialog, container, false);

            String currentName = getArguments().getString("filename", "");
            
            ((Button)v.findViewById(R.id.cancel)).setOnClickListener(this);
            ((Button)v.findViewById(R.id.ok)).setOnClickListener(this);
            ((TextView)v.findViewById(R.id.user_input)).setText(currentName);

            mResult = false;
            return v;
        }
        
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.ok: {
                    mNewFilename = ((TextView)getView().findViewById(R.id.user_input)).getText().toString();
                    mResult = true;
                }
                case R.id.cancel: { // fallthought
                    dismiss();
                    mListener.onDismiss(this);
                }
            }
        }
        
        void setOnDismissListener(FileDetailFragment listener) {
            mListener = listener;
        }
        
        public String getNewFilename() {
            return mNewFilename;
        }
        
        // true if user click ok
        public boolean getResult() {
            return mResult;
        }
        
    }
}
