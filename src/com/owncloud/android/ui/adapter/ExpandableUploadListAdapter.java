package com.owncloud.android.ui.adapter;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.db.UploadDbHandler;
import com.owncloud.android.db.UploadDbHandler.UploadStatus;
import com.owncloud.android.db.UploadDbObject;
import com.owncloud.android.files.services.FileUploadService;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.UploadUtils;

/**
 * This Adapter populates a ListView with following types of uploads: pending,
 * active, completed. Filtering possible.
 * 
 */
public class ExpandableUploadListAdapter extends BaseExpandableListAdapter implements Observer {

    private static final String TAG = "ExpandableUploadListAdapter";
    private Activity mActivity;
    
    public ProgressListener mProgressListener; 
    UploadFileOperation mCurrentUpload;
    
    interface Refresh {
        public void refresh();
    }
    abstract class UploadGroup implements Refresh {
        UploadDbObject[] items;
        String name;
        public UploadGroup(String groupName) {
            this.name = groupName;            
            items = new UploadDbObject[0];
        }        
        public String getGroupName() {
            return name;
        }
        public Comparator<UploadDbObject> comparator = new Comparator<UploadDbObject>() {
            @Override
            public int compare(UploadDbObject lhs, UploadDbObject rhs) {
                return compareUploadTime(lhs, rhs);
            }
            private int compareUploadTime(UploadDbObject lhs, UploadDbObject rhs) {
                return rhs.getUploadTime().compareTo(lhs.getUploadTime());
            }
        };
        abstract public int getGroupIcon();
    }
    private UploadGroup[] mUploadGroups = null;
    UploadDbHandler mDb;

    FileActivity parentFileActivity;
    public void setFileActivity(FileActivity parentFileActivity) {
        this.parentFileActivity = parentFileActivity;
    }
    
    public ExpandableUploadListAdapter(Activity context) {
        Log_OC.d(TAG, "UploadListAdapter");
        mActivity = context;
        mDb = UploadDbHandler.getInstance(mActivity);
        mUploadGroups = new UploadGroup[3];
        mUploadGroups[0] = new UploadGroup("Current Uploads") {
            @Override
            public void refresh() {
                items = mDb.getCurrentAndPendingUploads();
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
                items = mDb.getFailedUploads();
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
                items = mDb.getFinishedUploads();
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
        mDb.addObserver(this);
        Log_OC.d(TAG, "registerDataSetObserver");
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);
        mDb.deleteObserver(this);
        Log_OC.d(TAG, "unregisterDataSetObserver");
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }
    
    private View getView(UploadDbObject[] uploadsItems, int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflator = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.upload_list_item, null);
        }
        if (uploadsItems != null && uploadsItems.length > position) {
            final UploadDbObject uploadObject = uploadsItems[position];

            TextView fileName = (TextView) view.findViewById(R.id.upload_name);
            String file = uploadObject.getOCFile().getFileName();
            fileName.setText(file);
            
            TextView localPath = (TextView) view.findViewById(R.id.upload_local_path);
            String path = uploadObject.getOCFile().getStoragePath();
            path = path.substring(0, path.length() - file.length() - 1);
            localPath.setText("Path: " + path);

            TextView fileSize = (TextView) view.findViewById(R.id.upload_file_size);
            fileSize.setText(DisplayUtils.bytesToHumanReadable(uploadObject.getOCFile().getFileLength()));

            TextView statusView = (TextView) view.findViewById(R.id.upload_status);
            String status;
            switch (uploadObject.getUploadStatus()) {
            case UPLOAD_IN_PROGRESS:
                status = mActivity.getResources().getString(R.string.uploader_upload_in_progress_ticker);
                ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.upload_progress_bar);
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
                mProgressListener = new ProgressListener(progressBar);
                if(parentFileActivity.getFileUploaderBinder() != null) {
                    mCurrentUpload = parentFileActivity.getFileUploaderBinder().getCurrentUploadOperation();
                    if(mCurrentUpload != null) {
                        mCurrentUpload.addDatatransferProgressListener(mProgressListener);
                        Log_OC.d(TAG, "added progress listener for current upload: " + mCurrentUpload);
                    } else {
                        Log_OC.w(TAG, "getFileUploaderBinder().getCurrentUploadOperation() return null. That is odd.");
                    }
                } else {
                    Log_OC.e(TAG, "UploadBinder == null. It should have been created on creating parentFileActivity"
                            + " which inherits from FileActivity. Fix that!");
                }
                break;
            case UPLOAD_FAILED_GIVE_UP:
                if (uploadObject.getLastResult() != null) {
                    status = "Upload failed: " + uploadObject.getLastResult().getLogMessage();
                } else {
                    status = "Upload failed.";
                }
                break;
            case UPLOAD_FAILED_RETRY:
                if(uploadObject.getLastResult() != null){
                    status = "Last failure: "
                        + uploadObject.getLastResult().getLogMessage();
                } else {
                    status = "Upload will be retried shortly.";
                }
                String laterReason = FileUploadService.getUploadLaterReason(mActivity, uploadObject);
                if(laterReason != null) {
                    //Upload failed once but is delayed now, show reason.
                    status += "\n" + laterReason;
                }
                break;
            case UPLOAD_LATER:
                status = FileUploadService.getUploadLaterReason(mActivity, uploadObject);
                break;
            case UPLOAD_SUCCEEDED:
                status = "Completed.";
                break;
            case UPLOAD_CANCELLED:
                status = "Upload cancelled.";
                break;
            case UPLOAD_PAUSED:
                status = "Upload paused.";
                break;
            default:
                status = uploadObject.getUploadStatus().toString();
                if(uploadObject.getLastResult() != null){
                    uploadObject.getLastResult().getLogMessage();
                } 
                break;
            }
            if(uploadObject.getUploadStatus() != UploadStatus.UPLOAD_IN_PROGRESS) {
                ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.upload_progress_bar);
                progressBar.setVisibility(View.GONE);
                if (parentFileActivity.getFileUploaderBinder() != null && mProgressListener != null
                        && mCurrentUpload != null) {
                    OCFile currentOcFile = mCurrentUpload.getFile();
                    parentFileActivity.getFileUploaderBinder().removeDatatransferProgressListener(mProgressListener,
                            uploadObject.getAccount(mActivity), currentOcFile);
                    mProgressListener = null;
                    mCurrentUpload = null;
                }            
            }
            statusView.setText(status);

            Button rightButton = (Button) view.findViewById(R.id.upload_right_button);
            if (UploadUtils.userCanRetryUpload(uploadObject)
                    && uploadObject.getUploadStatus() != UploadStatus.UPLOAD_SUCCEEDED) {
                rightButton.setText("\u21BA"); //Anticlockwise Open Circle Arrow U+21BA
                rightButton.setOnClickListener(new OnClickListener() {                
                    @Override
                    public void onClick(View v) {
                        parentFileActivity.getFileOperationsHelper().retryUpload(uploadObject);                                        
                    }
                });
            } else if (UploadUtils.userCanCancelUpload(uploadObject)) {
                rightButton.setText("\u274C"); //Cross Mark U+274C
                rightButton.setOnClickListener(new OnClickListener() {                
                    @Override
                    public void onClick(View v) {
                        parentFileActivity.getFileOperationsHelper().cancelTransference(uploadObject.getOCFile());                                        
                    }
                });
            } else {
                rightButton.setText("\u267B"); //Black Universal Recycling Symbol U+267B
                rightButton.setOnClickListener(new OnClickListener() {                
                    @Override
                    public void onClick(View v) {
                        parentFileActivity.getFileOperationsHelper().removeUploadFromList(uploadObject);                                        
                    }
                });
            }
            

            ImageView fileIcon = (ImageView) view.findViewById(R.id.imageView1);
            fileIcon.setImageResource(R.drawable.file);
            try {
                //TODO Wait for https://github.com/owncloud/android/pull/746 and add thumbnail.
                Bitmap b = ThumbnailsCacheManager.getBitmapFromDiskCache(uploadObject.getOCFile().getRemoteId());
                if (b != null) {
                    fileIcon.setImageBitmap(b);
                }
            } catch (NullPointerException e) {
            }            

            TextView uploadDate = (TextView) view.findViewById(R.id.upload_date);
            CharSequence dateString = DisplayUtils.getRelativeDateTimeString(mActivity, uploadObject.getUploadTime()
                    .getTimeInMillis(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
            uploadDate.setText(dateString);
        }

        return view;
    }

    

    @Override
    public boolean hasStableIds() {
        return false;
    }

    

    /**
     * Load upload items from {@link UploadDbHandler}.
     */
    private void loadUploadItemsFromDb() {
        Log_OC.d(TAG, "loadUploadItemsFromDb");
        
        for (UploadGroup group : mUploadGroups) {
            group.refresh();
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });

    }

    @Override
    public void update(Observable arg0, Object arg1) {
        Log_OC.d(TAG, "update");
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
            LayoutInflater infalInflater = (LayoutInflater) mActivity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.upload_list_group, null);
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
    
    private class ProgressListener implements OnDatatransferProgressListener {
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
}
