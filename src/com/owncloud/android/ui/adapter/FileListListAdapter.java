/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
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
package com.owncloud.android.ui.adapter;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.Vector;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.utils.DisplayUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;


/**
 * This Adapter populates a ListView with all files and folders in an ownCloud
 * instance.
 * 
 * @author Bartek Przybylski
 * @Author Tobias Kaminsky
 * 
 */
public class FileListListAdapter extends BaseAdapter implements ListAdapter {
    private final static String PERMISSION_SHARED_WITH_ME = "S";

    private Context mContext;
    private OCFile mFile = null;
    private Vector<OCFile> mFiles = null;

    private FileDataStorageManager mStorageManager;
    private Account mAccount;
    private ComponentsGetter mTransferServiceGetter;
    private final Object thumbnailDiskCacheLock = new Object();
    private DiskLruImageCache mThumbnailCache;
    private boolean mThumbnailCacheStarting = true;
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    private static final CompressFormat mCompressFormat = CompressFormat.JPEG;
    private static final int mCompressQuality = 70;
    private OwnCloudClient mClient;
    private Bitmap defaultImg;
        
    public FileListListAdapter(Context context, ComponentsGetter transferServiceGetter) {
        mContext = context;
        mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        mTransferServiceGetter = transferServiceGetter;
        defaultImg = BitmapFactory.decodeResource(mContext.getResources(), 
                    DisplayUtils.getResourceId("image/png", "default.png"));
        
        // Initialise disk cache on background thread
        new InitDiskCacheTask().execute();
    }
    
    class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            synchronized (thumbnailDiskCacheLock) {
                mThumbnailCache = new DiskLruImageCache(mContext, "thumbnailCache", 
                                    DISK_CACHE_SIZE, mCompressFormat, mCompressQuality);
                
                try {
                    OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount, mContext);
                    mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, mContext);
                } catch (AccountNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (OperationCanceledException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                mThumbnailCacheStarting = false; // Finished initialization
                thumbnailDiskCacheLock.notifyAll(); // Wake any waiting threads
            }
            return null;
        }
    }
    
    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<ThumbnailGenerationTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                ThumbnailGenerationTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                new WeakReference<ThumbnailGenerationTask>(bitmapWorkerTask);
        }

        public ThumbnailGenerationTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    class ThumbnailGenerationTask extends AsyncTask<OCFile, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private OCFile file;

        
        public ThumbnailGenerationTask(ImageView imageView) {
         // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(OCFile... params) {
            file = params[0];
            final String imageKey = String.valueOf(file.getRemoteId());

            // Check disk cache in background thread
            Bitmap thumbnail = getBitmapFromDiskCache(imageKey);

            // Not found in disk cache
            if (thumbnail == null) { 
                // Converts dp to pixel
                Resources r = mContext.getResources();
                int px = (int) Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, r.getDisplayMetrics()));
                
                if (file.isDown()){
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getStoragePath());
                    thumbnail = ThumbnailUtils.extractThumbnail(bitmap, px, px);
                    
                    // Add thumbnail to cache
                    addBitmapToCache(imageKey, thumbnail);

                } else {
                    // Download thumbnail from server
                    // Commented out as maybe changes to client library are needed
//                    DefaultHttpClient httpclient = new DefaultHttpClient();
//                    try {
//                        httpclient.getCredentialsProvider().setCredentials(
//                                new AuthScope(mClient.getBaseUri().toString().replace("https://", ""), 443), 
//                                new UsernamePasswordCredentials(mClient.getCredentials().getUsername(), mClient.getCredentials().getAuthToken()));
//                        
//
//                        HttpGet httpget = new HttpGet(mClient.getBaseUri() + "/ocs/v1.php/thumbnail?x=50&y=50&path=" + URLEncoder.encode(file.getRemotePath(), "UTF-8"));
//                        HttpResponse response = httpclient.execute(httpget);
//                        HttpEntity entity = response.getEntity();
//                        
//                        if (entity != null) {
//                            byte[] bytes = EntityUtils.toByteArray(entity);
//                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                            thumbnail = ThumbnailUtils.extractThumbnail(bitmap, px, px);
//                            
//                            // Add thumbnail to cache
//                            if (thumbnail != null){
//                                addBitmapToCache(imageKey, thumbnail);
//                            }
//                        }
//                    } catch(Exception e){
//                        e.printStackTrace();
//                    }finally {
//                        httpclient.getConnectionManager().shutdown();
//                    }
                } 
            }
            return thumbnail;
        }
        
        protected void onPostExecute(Bitmap bitmap){
            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final ThumbnailGenerationTask bitmapWorkerTask =
                        getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
  
    public void addBitmapToCache(String key, Bitmap bitmap) {
        synchronized (thumbnailDiskCacheLock) {
            if (mThumbnailCache != null && mThumbnailCache.getBitmap(key) == null) {
                mThumbnailCache.put(key, bitmap);
            }
        }
    }

    public Bitmap getBitmapFromDiskCache(String key) {
        synchronized (thumbnailDiskCacheLock) {
            // Wait while disk cache is started from background thread
            while (mThumbnailCacheStarting) {
                try {
                    thumbnailDiskCacheLock.wait();
                } catch (InterruptedException e) {}
            }
            if (mThumbnailCache != null) {
                return (Bitmap) mThumbnailCache.getBitmap(key);
            }
        }
        return null;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        return mFiles != null ? mFiles.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        if (mFiles == null || mFiles.size() <= position)
            return null;
        return mFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (mFiles == null || mFiles.size() <= position)
            return 0;
        return mFiles.get(position).getFileId();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflator = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.list_item, null);
        }
         
        if (mFiles != null && mFiles.size() > position) {
            OCFile file = mFiles.get(position);
            TextView fileName = (TextView) view.findViewById(R.id.Filename);           
            String name = file.getFileName();

            fileName.setText(name);
            ImageView fileIcon = (ImageView) view.findViewById(R.id.imageView1);
            ImageView sharedIconV = (ImageView) view.findViewById(R.id.sharedIcon);
            ImageView sharedWithMeIconV = (ImageView) view.findViewById(R.id.sharedWithMeIcon);
            sharedWithMeIconV.setVisibility(View.GONE);

            ImageView localStateView = (ImageView) view.findViewById(R.id.imageView2);
            localStateView.bringToFront();
            FileDownloaderBinder downloaderBinder = mTransferServiceGetter.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mTransferServiceGetter.getFileUploaderBinder();
            if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file)) {
                localStateView.setImageResource(R.drawable.downloading_file_indicator);
                localStateView.setVisibility(View.VISIBLE);
            } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file)) {
                localStateView.setImageResource(R.drawable.uploading_file_indicator);
                localStateView.setVisibility(View.VISIBLE);
            } else if (file.isDown()) {
                localStateView.setImageResource(R.drawable.local_file_indicator);
                localStateView.setVisibility(View.VISIBLE);
            } else {
                localStateView.setVisibility(View.INVISIBLE);
            }
            
            TextView fileSizeV = (TextView) view.findViewById(R.id.file_size);
            TextView lastModV = (TextView) view.findViewById(R.id.last_mod);
            ImageView checkBoxV = (ImageView) view.findViewById(R.id.custom_checkbox);
            
            if (!file.isFolder()) {
                fileSizeV.setVisibility(View.VISIBLE);
                fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
                lastModV.setVisibility(View.VISIBLE);
                lastModV.setText(DisplayUtils.unixTimeToHumanReadable(file.getModificationTimestamp()));
                // this if-else is needed even thoe fav icon is visible by default
                // because android reuses views in listview
                if (!file.keepInSync()) {
                    view.findViewById(R.id.imageView3).setVisibility(View.GONE);
                } else {
                    view.findViewById(R.id.imageView3).setVisibility(View.VISIBLE);
                }
                
                ListView parentList = (ListView)parent;
                if (parentList.getChoiceMode() == ListView.CHOICE_MODE_NONE) { 
                    checkBoxV.setVisibility(View.GONE);
                } else {
                    if (parentList.isItemChecked(position)) {
                        checkBoxV.setImageResource(android.R.drawable.checkbox_on_background);
                    } else {
                        checkBoxV.setImageResource(android.R.drawable.checkbox_off_background);
                    }
                    checkBoxV.setVisibility(View.VISIBLE);
                }               
                
                // get Thumbnail if file is image
                if (file.isImage()){
                     // Thumbnail in Cache?
                    Bitmap thumbnail = getBitmapFromDiskCache(String.valueOf(file.getRemoteId()));
                    if (thumbnail != null){
                        fileIcon.setImageBitmap(thumbnail);
                    } else {
                        // generate new Thumbnail
                        if (cancelPotentialWork(file, fileIcon)) {
                            final ThumbnailGenerationTask task = new ThumbnailGenerationTask(fileIcon);
                            final AsyncDrawable asyncDrawable =
                                    new AsyncDrawable(mContext.getResources(), defaultImg, task);
                            fileIcon.setImageDrawable(asyncDrawable);
                            task.execute(file);
                        }
                    }
                }

                if (checkIfFileIsSharedWithMe(file)) {
                    sharedWithMeIconV.setVisibility(View.VISIBLE);
                }
            } 
            else {
                fileSizeV.setVisibility(View.INVISIBLE);
                //fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
                lastModV.setVisibility(View.VISIBLE);
                lastModV.setText(DisplayUtils.unixTimeToHumanReadable(file.getModificationTimestamp()));
                checkBoxV.setVisibility(View.GONE);
                view.findViewById(R.id.imageView3).setVisibility(View.GONE);

                if (checkIfFileIsSharedWithMe(file)) {
                    fileIcon.setImageResource(R.drawable.shared_with_me_folder);
                    sharedWithMeIconV.setVisibility(View.VISIBLE);
                } else {
                    fileIcon.setImageResource(DisplayUtils.getResourceId(file.getMimetype(), file.getFileName()));
                }

                // If folder is sharedByLink, icon folder must be changed to
                // folder-public one
                if (file.isShareByLink()) {
                    fileIcon.setImageResource(R.drawable.folder_public);
                }
            }

            if (file.isShareByLink()) {
                sharedIconV.setVisibility(View.VISIBLE);
            } else {
                sharedIconV.setVisibility(View.GONE);
            }
        }

        return view;
    }
    
    public static boolean cancelPotentialWork(OCFile file, ImageView imageView) {
        final ThumbnailGenerationTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final OCFile bitmapData = bitmapWorkerTask.file;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == null || bitmapData != file) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }
    
    private static ThumbnailGenerationTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
         }
         return null;
     }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return (mFiles == null || mFiles.isEmpty());
    }

    /**
     * Change the adapted directory for a new one
     * @param directory                 New file to adapt. Can be NULL, meaning "no content to adapt".
     * @param updatedStorageManager     Optional updated storage manager; used to replace mStorageManager if is different (and not NULL)
     */
    public void swapDirectory(OCFile directory, FileDataStorageManager updatedStorageManager) {
        mFile = directory;
        if (updatedStorageManager != null && updatedStorageManager != mStorageManager) {
            mStorageManager = updatedStorageManager;
            mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        }
        if (mStorageManager != null) {
            mFiles = mStorageManager.getFolderContent(mFile);
        } else {
            mFiles = null;
        }
        notifyDataSetChanged();
    }
    
    /**
     * Check if parent folder does not include 'S' permission and if file/folder
     * is shared with me
     * 
     * @param file: OCFile
     * @return boolean: True if it is shared with me and false if it is not
     */
    private boolean checkIfFileIsSharedWithMe(OCFile file) {
        return (mFile.getPermissions() != null && !mFile.getPermissions().contains(PERMISSION_SHARED_WITH_ME)
                && file.getPermissions() != null && file.getPermissions().contains(PERMISSION_SHARED_WITH_ME));
    }
}
