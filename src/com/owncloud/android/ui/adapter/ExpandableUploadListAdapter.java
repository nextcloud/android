/**
 *   ownCloud Android client application
 *
 *   @author LukeOwncloud
 *   @author masensio
 *   Copyright (C) 2015 ownCloud Inc.
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

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.FileUploadService;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Observable;
import java.util.Observer;

/**
 * This Adapter populates a ListView with following types of uploads: pending,
 * active, completed. Filtering possible.
 * 
 */
public class ExpandableUploadListAdapter extends BaseExpandableListAdapter implements Observer {

    private static final String TAG = ExpandableUploadListAdapter.class.getSimpleName();
    private FileActivity mParentActivity;

    private UploadsStorageManager mUploadsStorageManager;
    
    public ProgressListener mProgressListener; 
    private UploadFileOperation mCurrentUpload;
    
    interface Refresh {
        public void refresh();
    }
    abstract class UploadGroup implements Refresh {
        OCUpload[] items;
        String name;
        public UploadGroup(String groupName) {
            this.name = groupName;            
            items = new OCUpload[0];
        }        
        public String getGroupName() {
            return name;
        }
        public Comparator<OCUpload> comparator = new Comparator<OCUpload>() {
            @Override
            public int compare(OCUpload lhs, OCUpload rhs) {
                return compareUploadTime(lhs, rhs);
            }
            private int compareUploadTime(OCUpload lhs, OCUpload rhs) {
                return rhs.getUploadTime().compareTo(lhs.getUploadTime());
            }
        };
        abstract public int getGroupIcon();
    }
    private UploadGroup[] mUploadGroups = null;

    public ExpandableUploadListAdapter(FileActivity parentActivity) {
        Log_OC.d(TAG, "ExpandableUploadListAdapter");
        mParentActivity = parentActivity;
        mUploadsStorageManager = new UploadsStorageManager(mParentActivity.getContentResolver());
        mUploadGroups = new UploadGroup[3];
        mUploadGroups[0] = new UploadGroup("Current Uploads") {
            @Override
            public void refresh() {
                items = mUploadsStorageManager.getCurrentAndPendingUploads();
                Arrays.sort(items, comparator);
            }
            @Override
            public int getGroupIcon() {
                return R.drawable.upload_in_progress;
            }
        };
        mUploadGroups[1] = new UploadGroup("Failed Uploads"){
            @Override
            public void refresh() {
                items = mUploadsStorageManager.getFailedUploads();
                Arrays.sort(items, comparator);
            }
            @Override
            public int getGroupIcon() {
                return R.drawable.upload_failed;
            }

        };
        mUploadGroups[2] = new UploadGroup("Finished Uploads"){
            @Override
            public void refresh() {
                items = mUploadsStorageManager.getFinishedUploads();
                Arrays.sort(items, comparator);
            }
            @Override
            public int getGroupIcon() {
                return R.drawable.upload_finished;
            }

        };
        loadUploadItemsFromDb();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
        mUploadsStorageManager.addObserver(this);
        Log_OC.d(TAG, "registerDataSetObserver");
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);
        mUploadsStorageManager.deleteObserver(this);
        Log_OC.d(TAG, "unregisterDataSetObserver");
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }
    
    private View getView(OCUpload[] uploadsItems, int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflator =
                    (LayoutInflater) mParentActivity.getSystemService(
                            Context.LAYOUT_INFLATER_SERVICE
                    );
            view = inflator.inflate(R.layout.upload_list_item, null);
        }


        if (uploadsItems != null && uploadsItems.length > position) {
            final OCUpload upload = uploadsItems[position];
            final OCFile uploadOCFile = upload.getOCFile();

            TextView fileTextView = (TextView) view.findViewById(R.id.upload_name);
            String fileName = uploadOCFile.getFileName();
            fileTextView.setText(fileName);
            
            TextView localPath = (TextView) view.findViewById(R.id.upload_local_path);
            String path = uploadOCFile.getStoragePath();
            path = path == null ? "" : path.substring(0, path.length() - fileName.length() - 1);
            localPath.setText("Path: " + path);

            TextView fileSize = (TextView) view.findViewById(R.id.upload_file_size);
            fileSize.setText(DisplayUtils.bytesToHumanReadable(uploadOCFile.getFileLength()));

            TextView statusView = (TextView) view.findViewById(R.id.upload_status);
            String status;
            switch (upload.getUploadStatus()) {
                case UPLOAD_IN_PROGRESS:
                    status = mParentActivity.getString(R.string.uploader_upload_in_progress_ticker);
                    ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.upload_progress_bar);
                    progressBar.setProgress(0);
                    progressBar.setVisibility(View.VISIBLE);
                    mProgressListener = new ProgressListener(progressBar);
                    if(mParentActivity.getFileUploaderBinder() != null) {
                        mParentActivity.getFileUploaderBinder().addDatatransferProgressListener(mProgressListener,
                                mParentActivity.getAccount(), uploadOCFile);
                    } else {
                        Log_OC.e(TAG, "UploadBinder == null. It should have been created on creating mParentActivity"
                                + " which inherits from FileActivity. Fix that!");
                        Log_OC.e(TAG, "PENDING BINDING for upload = " + upload.getLocalPath());
                    }
                    break;
                case UPLOAD_FAILED_GIVE_UP:
                    if (upload.getLastResult() != null) {
                        switch (upload.getLastResult()) {
                            case CREDENTIAL_ERROR:
                                status = mParentActivity.getString(
                                        R.string.uploads_view_upload_status_failed_credentials_error);

                                view.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // let the user update credentials with one click
                                        Intent updateAccountCredentials = new Intent(mParentActivity,
                                                AuthenticatorActivity.class);
                                        updateAccountCredentials.putExtra(
                                                AuthenticatorActivity.EXTRA_ACCOUNT, upload.getAccount(mParentActivity));
                                        updateAccountCredentials.putExtra(
                                                AuthenticatorActivity.EXTRA_ACTION,
                                                AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN);
                                        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                        updateAccountCredentials.addFlags(Intent.FLAG_FROM_BACKGROUND);
                                        mParentActivity.startActivity(updateAccountCredentials);
                                    }
                                });
                                break;
                            case FOLDER_ERROR:
                                status = mParentActivity.getString(
                                        R.string.uploads_view_upload_status_failed_folder_error);
                                break;
                            case FILE_ERROR:
                                status = mParentActivity.getString(
                                        R.string.uploads_view_upload_status_failed_file_error);
                                break;
                            case PRIVILEDGES_ERROR:
                                status = mParentActivity.getString(
                                        R.string.uploads_view_upload_status_failed_priviledges_error);
                                break;
                            default:
                                status = "Upload failed: " + upload.getLastResult().toString();
                                break;
                        }
                    } else {
                        status = "Upload failed.";
                    }
                    break;
                case UPLOAD_FAILED_RETRY:
                    if (upload.getLastResult() == UploadResult.NETWORK_CONNECTION) {
                        status = mParentActivity.getString(R.string.uploads_view_upload_status_failed_connection_error);
                    } else {
                        status =  mParentActivity.getString(R.string.uploads_view_upload_status_failed_retry);
                    }
                    String laterReason = FileUploadService.getUploadLaterReason(mParentActivity, upload);
                    if(laterReason != null) {
                        //Upload failed once but is delayed now, show reason.
                        status += "\n" + laterReason;
                    }
                    break;
                case UPLOAD_LATER:
                    status = FileUploadService.getUploadLaterReason(mParentActivity, upload);
                    break;
                case UPLOAD_SUCCEEDED:
                    status =  mParentActivity.getString(R.string.uploads_view_upload_status_succeeded);
                    break;
                case UPLOAD_CANCELLED:
                    status =  mParentActivity.getString(R.string.uploads_view_upload_status_cancelled);
                    break;
                case UPLOAD_PAUSED:
                    status =  mParentActivity.getString(R.string.uploads_view_upload_status_paused);
                    break;
                default:
                    status = upload.getUploadStatus().toString();
                    if(upload.getLastResult() != null){
                        upload.getLastResult().toString();
                    }
                    break;
            }
            if(upload.getUploadStatus() != UploadStatus.UPLOAD_IN_PROGRESS) {
                ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.upload_progress_bar);
                progressBar.setVisibility(View.GONE);
                if (mParentActivity.getFileUploaderBinder() != null && mProgressListener != null
                        && mCurrentUpload != null) {
                    OCFile currentOcFile = mCurrentUpload.getFile();
                    mParentActivity.getFileUploaderBinder().removeDatatransferProgressListener(mProgressListener,
                            upload.getAccount(mParentActivity), currentOcFile);
                    mProgressListener = null;
                    mCurrentUpload = null;
                }            
            }
            statusView.setText(status);

            ImageButton rightButton = (ImageButton) view.findViewById(R.id.upload_right_button);
            if (upload.userCanRetryUpload()
                    && upload.getUploadStatus() != UploadStatus.UPLOAD_SUCCEEDED) {
                //Refresh   - TODO test buttons in Android 4.x
                rightButton.setImageResource(R.drawable.ic_refresh);
                rightButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mParentActivity.getFileOperationsHelper().retryUpload(upload);
                    }
                });
            } else if (upload.userCanCancelUpload()) {
                //Cancel
                rightButton.setImageResource(R.drawable.ic_cancel);
                rightButton.setOnClickListener(new OnClickListener() {                
                    @Override
                    public void onClick(View v) {
                        mParentActivity.getFileOperationsHelper().cancelTransference(uploadOCFile);
                    }
                });
            } else {
                //Delete
                rightButton.setImageResource(R.drawable.ic_delete);
                rightButton.setOnClickListener(new OnClickListener() {                
                    @Override
                    public void onClick(View v) {
                        mParentActivity.getFileOperationsHelper().removeUploadFromList(upload);
                    }
                });
            }
            

            ImageView fileIcon = (ImageView) view.findViewById(R.id.thumbnail);
            fileIcon.setImageResource(R.drawable.file);

            /** Cancellation needs do be checked and done before changing the drawable in fileIcon, or
             * {@link ThumbnailsCacheManager#cancelPotentialWork} will NEVER cancel any task.
             **/
            boolean allowedToCreateNewThumbnail = (ThumbnailsCacheManager.cancelPotentialWork(uploadOCFile,
                    fileIcon));

            if (uploadOCFile.isImage() && uploadOCFile.getStoragePath()!= null){
                File file = new File(uploadOCFile.getStoragePath());
                // Thumbnail in Cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                        String.valueOf(file.hashCode()));
                if (thumbnail != null) {
                    fileIcon.setImageBitmap(thumbnail);
                } else {
                    // generate new Thumbnail
                    if (allowedToCreateNewThumbnail) {
                        final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                new ThumbnailsCacheManager.ThumbnailGenerationTask(fileIcon);
                        if (thumbnail == null) {
                            thumbnail = ThumbnailsCacheManager.mDefaultImg;
                        }
                        final ThumbnailsCacheManager.AsyncDrawable asyncDrawable =
                                new ThumbnailsCacheManager.AsyncDrawable(
                                        mParentActivity.getResources(),
                                        thumbnail,
                                        task
                                );
                        fileIcon.setImageDrawable(asyncDrawable);
                        task.execute(file);
                        Log_OC.v(TAG, "Executing task to generate a new thumbnail");
                    }
                }

                if (uploadOCFile.getMimetype().equalsIgnoreCase("image/png")) {
                    fileIcon.setBackgroundColor(mParentActivity.getResources()
                            .getColor(R.color.background_color));
                }
            } else {
                fileIcon.setImageResource(MimetypeIconUtil.getFileTypeIconId(upload.getMimeType(),
                        uploadOCFile.getFileName()));
            }
            TextView uploadDate = (TextView) view.findViewById(R.id.upload_date);
            CharSequence dateString = DisplayUtils.getRelativeDateTimeString(
                    mParentActivity,
                    upload.getUploadTime().getTimeInMillis(),
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    0
            );
            uploadDate.setText(dateString);
        }

        return view;
    }

    

    @Override
    public boolean hasStableIds() {
        return false;
    }


    /**
     * Load upload items from {@link UploadsStorageManager}.
     */
    private void loadUploadItemsFromDb() {
        Log_OC.d(TAG, "loadUploadItemsFromDb");
        
        for (UploadGroup group : mUploadGroups) {
            group.refresh();
        }

        notifyDataSetChanged();
    }

    @Override
    public void update(Observable arg0, Object arg1) {
        Log_OC.d(TAG, "update");
        loadUploadItemsFromDb();
    }


    public void refreshView() {
        Log_OC.d(TAG, "refreshView");
        loadUploadItemsFromDb();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mUploadGroups[(int) getGroupId(groupPosition)].items[childPosition];
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
            ViewGroup parent) {
        return getView(mUploadGroups[(int) getGroupId(groupPosition)].items, childPosition, convertView, parent);
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mUploadGroups[(int) getGroupId(groupPosition)].items.length;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mUploadGroups[(int) getGroupId(groupPosition)];
    }

    @Override
    public int getGroupCount() {
        int size = 0;
        for (UploadGroup uploadGroup : mUploadGroups) {
            if(uploadGroup.items.length > 0) {
                size++;
            }
        }
        return size;
    }

    /**
     * Returns the groupId (that is, index in mUploadGroups) for group at position groupPosition (0-based).
     * Could probably be done more intuitive but this tested methods works as intended.
     */
    @Override
    public long getGroupId(int groupPosition) {
        int id = -1;
        for (int i = 0; i <= groupPosition; ) {
            id++;
            if(mUploadGroups[id].items.length > 0){
                i++;
            }
        }
        return id;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        //force group to stay unfolded
        ExpandableListView listView = (ExpandableListView) parent;
        listView.expandGroup(groupPosition);
        
        listView.setGroupIndicator(null);
        UploadGroup group = (UploadGroup) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater inflaInflater = (LayoutInflater) mParentActivity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflaInflater.inflate(R.layout.upload_list_group, null);
        }
        TextView tv = (TextView) convertView.findViewById(R.id.uploadListGroupName);
        tv.setText(group.getGroupName());
//        ImageView icon = (ImageView) convertView.findViewById(R.id.uploadListGroupIcon);
//        icon.setImageResource(group.getGroupIcon());
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
    
    public class ProgressListener implements OnDatatransferProgressListener {
        int mLastPercent = 0;
        WeakReference<ProgressBar> mProgressBar = null;
        
        ProgressListener(ProgressBar progressBar) {
            mProgressBar = new WeakReference<ProgressBar>(progressBar);
        }
        
        @Override
        public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String filename) {
            int percent = (int)(100.0*((double)totalTransferredSoFar)/((double)totalToTransfer));
            if (percent != mLastPercent) {
                ProgressBar pb = mProgressBar.get();
                if (pb != null) {
                    pb.setProgress(percent);
                    pb.postInvalidate();
                }
            }
            mLastPercent = percent;
        }

    };

    public void addBinder(){
        notifyDataSetChanged();
    }
}
